package org.tech_solutions.application.imports.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;
import org.tech_solutions.application.importedtransactions.repository.ImportedTransactionRepository;
import org.tech_solutions.application.imports.ImportValidationException;
import org.tech_solutions.application.imports.UploadOperationException;
import org.tech_solutions.application.imports.enums.ImportStatus;
import org.tech_solutions.application.imports.model.ImportFile;
import org.tech_solutions.application.imports.repository.ImportFileRepository;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.shared.exception.EntityNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
    private final ImportedTransactionRepository importedTransactionRepository;
    private final CurrentUserService currentUserService;

    public ImportFileService(
            MinioClient minioClient,
            ImportFileRepository importFileRepository,
            ImportedTransactionRepository importedTransactionRepository,
            CurrentUserService currentUserService
    ) {
        this.minioClient = minioClient;
        this.importFileRepository = importFileRepository;
        this.importedTransactionRepository = importedTransactionRepository;
        this.currentUserService = currentUserService;
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
        importFile.setUser(currentUserService.requireCurrentUser());
        importFile.setImportedAt(LocalDateTime.now());
        if (importFile.getStatus() == null) {
            importFile.setStatus(ImportStatus.PROCESSING);
        }
        return importFileRepository.save(importFile);
    }

    public List<ImportFile> listAll() {
        return importFileRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public List<ImportFile> listByUser(Long userId) {
        return importFileRepository.findByUserId(currentUserService.requireCurrentUserId());
    }

    public ImportFile findById(Long id) {
        ImportFile importFile = importFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Importacao nao encontrada"));
        assertOwnedByCurrentUser(importFile);
        return importFile;
    }

    public ImportFile update(Long id, ImportFile updated, Long userId) {
        ImportFile current = findById(id);
        current.setUser(currentUserService.requireCurrentUser());
        current.setFileName(updated.getFileName());
        return importFileRepository.save(current);
    }

    public void delete(Long id) {
        importFileRepository.delete(findById(id));
    }

    public void upload(MultipartFile file, Long userId) {
        if(file == null) {
            LOGGER.error("'file' foi recebido como nulo");
            throw new ImportValidationException("Arquivo recebido como vazio pelo sistema");
        }

        String originalFilename = file.getOriginalFilename();
        String filename = this.generateNewFilename(originalFilename);
        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new UploadOperationException("Nao foi possivel ler o arquivo enviado");
        }

        ImportFile fileInfos = new ImportFile(filename);

        try {
            LOGGER.info("Carregando arquivo {} no bucket {}", originalFilename, bucket);
            var response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                            .contentType(file.getContentType())
                            .build()
            );
            LOGGER.info("response: {}", response.object());

            LOGGER.info("Arquivo carregado no bucket {}", bucket);

            LOGGER.info("Salvando informações sobre o upload do arquivo na base relacional...");
            ImportFile savedImport = this.create(fileInfos, userId);
            persistImportedTransactions(savedImport, fileBytes);
            savedImport.setStatus(ImportStatus.COMPLETED);
            importFileRepository.save(savedImport);

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
            currentUserService.requireCurrentUserId(),
            ImportStatus.COMPLETED,
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
                    String[] columns = parseCsvLine(line);
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

    private void persistImportedTransactions(ImportFile importFile, byte[] fileBytes) {
        List<ImportedTransaction> importedTransactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8)
        )) {
            String header = reader.readLine();
            if (header == null) {
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = parseCsvLine(line);
                if (columns.length < 3) {
                    continue;
                }

                LocalDate rawDate = parseDate(columns[0]);
                BigDecimal rawAmount = parseAmount(columns[2]);
                if (rawDate == null || rawAmount == null) {
                    continue;
                }

                ImportedTransaction importedTransaction = new ImportedTransaction();
                importedTransaction.setImportFile(importFile);
                importedTransaction.setRawDescription(columns[1].trim());
                importedTransaction.setRawAmount(rawAmount);
                importedTransaction.setRawDate(rawDate);
                importedTransaction.setProcessed(false);
                importedTransactions.add(importedTransaction);
            }
        } catch (IOException e) {
            throw new UploadOperationException("Nao foi possivel processar o CSV importado");
        }

        if (!importedTransactions.isEmpty()) {
            importedTransactionRepository.saveAll(importedTransactions);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
                continue;
            }

            if (character == ',' && !insideQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(character);
        }

        columns.add(current.toString());
        return columns.toArray(String[]::new);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            String normalized = value.trim()
                    .replace("R$", "")
                    .replace(".", "")
                    .replace(",", ".");
            return new BigDecimal(normalized);
        } catch (Exception ignored) {
            return null;
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

    private void assertOwnedByCurrentUser(ImportFile importFile) {
        Long currentUserId = currentUserService.requireCurrentUserId();
        if (importFile.getUser() == null || !currentUserId.equals(importFile.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Recurso nao pertence ao usuario autenticado");
        }
    }
}


