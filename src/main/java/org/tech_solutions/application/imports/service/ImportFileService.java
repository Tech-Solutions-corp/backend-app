package org.tech_solutions.application.imports.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tech_solutions.application.imports.ImportValidationException;
import org.tech_solutions.application.imports.UploadOperationException;
import org.tech_solutions.application.imports.enums.ImportStatus;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.repository.ImportFileRepository;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ImportFileService {

    @Value("${minio.bucket}")
    private String bucket;
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportFileService.class);

    private final MinioClient minioClient;
    private final ImportFileRepository importFileRepository;
    private final UserRepository userRepository;

    public ImportFileService(MinioClient minioClient, ImportFileRepository importFileRepository, UserRepository userRepository) {
        this.minioClient = minioClient;
        this.importFileRepository = importFileRepository;
        this.userRepository = userRepository;
    }

    public void inicializarBucket() {
        try {
            boolean existe = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!existe) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );
                LOGGER.info("Bucket '{}' criado com sucesso.", bucket);
            } else {
                LOGGER.info("Bucket '{}' já existe, nenhuma ação necessária.", bucket);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar bucket no MinIO: " + e.getMessage(), e);
        }
    }

    public ImportFile create(ImportFile importFile, Long userId) {
        importFile.setUser(findUser(userId));
        importFile.setImportedAt(LocalDateTime.now());
        if (importFile.getStatus() == null) {
            importFile.setStatus(ImportStatus.PROCESSING);
        }
        return importFileRepository.save(importFile);
    }

    public List<ImportFile> listAll() {
        return importFileRepository.findAll();
    }

    public List<ImportFile> listByUser(Long userId) {
        findUser(userId);
        return importFileRepository.findByUserId(userId);
    }

    public ImportFile findById(Long id) {
        return importFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importacao nao encontrada"));
    }

    public ImportFile update(Long id, ImportFile updated, Long userId) {
        ImportFile current = findById(id);
        current.setUser(findUser(userId));
        current.setFileName(updated.getFileName());
        current.setStatus(updated.getStatus() == null ? current.getStatus() : updated.getStatus());
        return importFileRepository.save(current);
    }

    public void delete(Long id) {
        importFileRepository.delete(findById(id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
    }

    public void upload(MultipartFile file, Long userId) {
        if(file == null) {
            LOGGER.error("'file' foi recebido como nulo");
            throw new ImportValidationException("Arquivo recebido como vazio pelo sistema");
        }

        String originalFilename = file.getOriginalFilename();
        String filename = this.generateNewFilename(originalFilename);

        try {
            LOGGER.info("Carregando arquivo {} no bucket {}", originalFilename, bucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            LOGGER.info("Arquivo carregado no bucket {}", bucket);

            LOGGER.info("Salvando informações sobre o upload do arquivo na base relacional...");
            ImportFile fileInfos = new ImportFile(originalFilename);
            this.create(fileInfos, userId);
            LOGGER.info("Informações sobre upload de arquivo persistidas");
        } catch (InsufficientDataException | IOException e) {
            LOGGER.error("Erro na leitura do arquivo: {}", originalFilename, e);
            throw new UploadOperationException("Ocorreu um erro ao ler arquivo recebido");
        } catch (InvalidKeyException e) {
            LOGGER.error("Erro na leitura do arquivo, credenciais inválidas na assinatura da requisição", e);
            throw new UploadOperationException("Ocorreu um erro ao ler arquivo recebido");
        } catch (ErrorResponseException e) {
            LOGGER.error("MinIO rejeitou o upload: {}", e.errorResponse().message(), e);
            throw new UploadOperationException("Ocorreu um erro ao carregar arquivo");
        } catch (Exception e) {
            LOGGER.error("Erro ao carreegar arquivo", e);
            throw new UploadOperationException("Ocorreu um erro ao carregar arquivo");
        }
    }

    private String generateNewFilename(String originalName) {
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : "";

        if (!extension.equals(".csv")) {
            LOGGER.error("arquivo possui extensão inválida, extensão recebida: {}", extension);
            throw new ImportValidationException("Tipo de arquivo não permitido");
        }

        return UUID.randomUUID() + "_" + originalName;
    }

}


