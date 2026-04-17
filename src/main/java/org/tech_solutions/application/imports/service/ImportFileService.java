package org.tech_solutions.application.imports.service;

import io.minio.*;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        ImportFile fileInfos = new ImportFile(filename);

        try {
            LOGGER.info("Carregando arquivo {} no bucket {}", originalFilename, bucket);
            var response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            LOGGER.info("response: {}", response.object());

            LOGGER.info("Arquivo carregado no bucket {}", bucket);

            LOGGER.info("Salvando informações sobre o upload do arquivo na base relacional...");
            fileInfos.setStatus(ImportStatus.COMPLETED);
            this.create(fileInfos, userId);

            LOGGER.info("Informações sobre upload de arquivo persistidas");
        } catch (Exception e) {
            fileInfos.setStatus(ImportStatus.FAILED);
            this.create(fileInfos, userId);
            this.handleMinioImportException(e, originalFilename);
        }
    }

    public List<String> fetchCsvDescriptions(Long userId,
                                             LocalDate startDate,
                                             LocalDate endDate) {
        List<ImportFile> files = importFileRepository.findCompletedByUserAndPeriod(
                userId,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        List<String> descriptions = new ArrayList<>();

        for (ImportFile file : files) {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(file.getFileName())
                            .build()
            )) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)
                );

                // Pula o header
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",");
                    // Adapta os índices conforme a estrutura real do seu CSV
                    // Assumindo: data, descrição, valor
                    if (columns.length >= 3) {
                        String desc = "%s | %s | R$%s".formatted(
                                columns[0].trim(),  // data
                                columns[1].trim(),  // descrição
                                columns[2].trim()   // valor
                        );
                        descriptions.add(desc);
                    }
                }
            } catch (Exception e) {
                handleMinioGetException(e);
                LOGGER.error("Erro ao ler arquivo MinIO: {}", file.getFileName());
            }
        }

        return descriptions;
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

    private void handleMinioImportException(Exception e, String filename) {
        if (e instanceof InsufficientDataException || e instanceof IOException) {
            LOGGER.error("Erro na leitura do arquivo: {}", filename, e);
        } else if (e instanceof InvalidKeyException) {
            LOGGER.error("Erro na leitura do arquivo, credenciais inválidas na assinatura da requisição", e);
        } else if (e instanceof ErrorResponseException ex) {
            LOGGER.error("MinIO rejeitou o upload: {}", ex.errorResponse().message(), e);
        } else {
            LOGGER.error("Erro ao carregar arquivo", e);
        }
        throw new UploadOperationException("Ocorreu um erro ao carregar arquivo");
    }

    private void handleMinioGetException(Exception e) {
        switch (e) {
            case ErrorResponseException err ->
                    LOGGER.error("Erro MinIO (objeto/bucket): {} ", err.errorResponse().message());
            case InsufficientDataException ignored ->
                    LOGGER.error("Stream incompleto ao ler do MinIO");
            case IOException ignored -> LOGGER.error("Erro de IO ao processar arquivo");
            default -> LOGGER.error("Erro inesperado: {}", e.getMessage());
        }
    }
}


