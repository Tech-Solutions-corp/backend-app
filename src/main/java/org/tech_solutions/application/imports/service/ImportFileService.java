package org.tech_solutions.application.imports.service;

import org.springframework.ai.chat.client.ChatClient;
import org.tech_solutions.application.categories.model.Category;
import org.tech_solutions.application.categories.repository.CategoryRepository;
import org.tech_solutions.application.accounts.model.Account;
import org.tech_solutions.application.accounts.repository.AccountRepository;
import org.tech_solutions.application.transactions.enums.TransactionType;
import org.tech_solutions.application.transactions.model.Transaction;
import org.tech_solutions.application.transactions.service.TransactionService;

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
    private final AccountRepository accountRepository;
    private final ChatClient chatClient;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    public ImportFileService(
            MinioClient minioClient,
            ImportFileRepository importFileRepository,
            ImportedTransactionRepository importedTransactionRepository,
            CurrentUserService currentUserService,
            AccountRepository accountRepository,
            ChatClient chatClient,
            CategoryRepository categoryRepository,
            TransactionService transactionService) {
        this.minioClient = minioClient;
        this.importFileRepository = importFileRepository;
        this.importedTransactionRepository = importedTransactionRepository;
        this.currentUserService = currentUserService;
        this.accountRepository = accountRepository;
        this.chatClient = chatClient;
        this.categoryRepository = categoryRepository;
        this.transactionService = transactionService;
    }

    public void inicializarBucket() {
        try {
            boolean existe = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build());

            if (!existe) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build());
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

    public void upload(MultipartFile file, Long userId, Long accountId) {
        if (file == null) {
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
                            .build());
            LOGGER.info("response: {}", response.object());

            LOGGER.info("Arquivo carregado no bucket {}", bucket);

            LOGGER.info("Salvando informações sobre o upload do arquivo na base relacional...");
            ImportFile savedImport = this.create(fileInfos, userId);
            persistImportedTransactions(savedImport, fileBytes, accountId);
            savedImport.setStatus(ImportStatus.COMPLETED);
            importFileRepository.save(savedImport);

            LOGGER.info("Informações sobre upload de arquivo persistidas");
        } catch (Exception e) {
            fileInfos.setStatus(ImportStatus.FAILED);
            this.create(fileInfos, userId);
            this.handleMinioImportException(e, originalFilename);
        }
    }

    public int reprocessImportedTransactions(Long importId) {
        ImportFile importFile = findById(importId);
        List<ImportedTransaction> pendingTransactions = importedTransactionRepository
                .findByImportFileIdAndProcessedFalseOrderByIdAsc(importId);

        if (pendingTransactions.isEmpty()) {
            return 0;
        }

        List<ParsedCsvRow> csvRows = readCsvRowsFromMinio(importFile);
        if (csvRows.isEmpty()) {
            throw new UploadOperationException("Nao foi possivel ler o CSV da importacao para reprocessar");
        }

        int total = Math.min(pendingTransactions.size(), csvRows.size());
        int processedCount = 0;
        List<ImportedTransaction> processedTransactions = new ArrayList<>();

        for (int index = 0; index < total; index++) {
            ImportedTransaction importedTransaction = pendingTransactions.get(index);
            ParsedCsvRow csvRow = csvRows.get(index);

            Account account = importedTransaction.getAccount();
            if (account == null) {
                throw new UploadOperationException("Conta original da importacao nao encontrada");
            }

            LocalDate rawDate = parseDate(csvRow.dateValue());
            BigDecimal rawAmount = parseAmount(csvRow.amountValue());
            if (rawDate == null || rawAmount == null) {
                continue;
            }

            rawAmount = rawAmount.abs();
            TransactionType transactionType = parseTransactionType(csvRow.typeValue(), rawAmount);
            String description = csvRow.descriptionValue();

            Category category = importedTransaction.getCategory();
            if (category == null && csvRow.categoryValue() != null && !csvRow.categoryValue().isBlank()) {
                category = findOrCreateCategory(csvRow.categoryValue().trim(), importFile.getUser().getId());
            }
            if (category == null) {
                category = findOrCreateCategory(
                        suggestCategoryWithAI(description, rawAmount, rawDate),
                        importFile.getUser().getId());
            }

            Transaction transaction = new Transaction();
            transaction.setTransactionDescription(description);
            transaction.setAmount(rawAmount);
            transaction.setTransactionDate(rawDate);
            transaction.setTransactionType(transactionType);
            transactionService.create(transaction, importFile.getUser().getId(), account.getId(), category != null ? category.getId() : null);

            importedTransaction.setProcessed(true);
            processedTransactions.add(importedTransaction);
            processedCount++;
        }

        if (processedCount > 0) {
            importedTransactionRepository.saveAll(processedTransactions);
            importFile.setStatus(ImportStatus.COMPLETED);
            importFileRepository.save(importFile);
        }

        return processedCount;
    }

    public List<String> fetchCsvDescriptions(Long userId,
            LocalDate startDate,
            LocalDate endDate) {
        List<ImportFile> files = importFileRepository.findCompletedByUserAndPeriod(
                currentUserService.requireCurrentUserId(),
                ImportStatus.COMPLETED,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59));

        List<String> descriptions = new ArrayList<>();

        for (ImportFile file : files) {
            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(file.getFileName())
                            .build())) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));

                // Pula o header
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] columns = parseCsvLine(line);
                    if (columns.length >= 3) {
                        String desc = "%s | %s | R$%s".formatted(
                                columns[0].trim(), // data
                                columns[1].trim(), // descrição
                                columns[2].trim() // valor
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

    private void persistImportedTransactions(ImportFile importFile, byte[] fileBytes, Long accountId) {
        List<ImportedTransaction> importedTransactions = new ArrayList<>();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new UploadOperationException("Conta não encontrada para o ID informado"));

        Long userId = importFile.getUser().getId();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
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

                ParsedCsvRow parsedRow = parseCsvRow(columns);
                if (parsedRow == null) {
                    continue;
                }

                LocalDate rawDate = parseDate(parsedRow.dateValue());
                BigDecimal rawAmount = parseAmount(parsedRow.amountValue());
                if (rawDate == null || rawAmount == null) {
                    continue;
                }

                rawAmount = rawAmount.abs();

                String description = parsedRow.descriptionValue();
                String suggestedCategoryName = resolveCategoryName(parsedRow.categoryValue(), description, rawAmount, rawDate);
                Category category = findOrCreateCategory(suggestedCategoryName, userId);

                TransactionType transactionType = parseTransactionType(parsedRow.typeValue(), rawAmount);

                Transaction transaction = new Transaction();
                transaction.setTransactionDescription(description);
                transaction.setAmount(rawAmount);
                transaction.setTransactionDate(rawDate);
                transaction.setTransactionType(transactionType);
                transactionService.create(transaction, userId, accountId, category.getId());

                ImportedTransaction importedTransaction = new ImportedTransaction();
                importedTransaction.setImportFile(importFile);
                importedTransaction.setAccount(account);
                importedTransaction.setRawDescription(description);
                importedTransaction.setRawAmount(rawAmount);
                importedTransaction.setRawDate(rawDate);
                importedTransaction.setCategory(category);
                importedTransaction.setProcessed(true);
                importedTransactions.add(importedTransaction);
            }
        } catch (IOException e) {
            throw new UploadOperationException("Nao foi possivel processar o CSV importado");
        }

        if (!importedTransactions.isEmpty()) {
            importedTransactionRepository.saveAll(importedTransactions);
        }
    }

    private List<ParsedCsvRow> readCsvRowsFromMinio(ImportFile importFile) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(importFile.getFileName())
                        .build());
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            List<ParsedCsvRow> rows = new ArrayList<>();
            String header = reader.readLine();
            if (header == null) {
                return rows;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = parseCsvLine(line);
                ParsedCsvRow row = parseCsvRow(columns);
                if (row != null) {
                    rows.add(row);
                }
            }

            return rows;
        } catch (Exception e) {
            handleMinioGetException(e);
            throw new UploadOperationException("Nao foi possivel ler o CSV da importacao");
        }
    }

    private String resolveCategoryName(String csvCategory, String description, BigDecimal amount, LocalDate date) {
        if (csvCategory != null && !csvCategory.isBlank()) {
            return csvCategory.trim();
        }
        return suggestCategoryWithAI(description, amount, date);
    }

    private TransactionType parseTransactionType(String csvType, BigDecimal amount) {
        if (csvType != null && !csvType.isBlank()) {
            String normalized = csvType.trim().toLowerCase();
            if (normalized.startsWith("inc") || normalized.contains("receita")) {
                return TransactionType.INCOME;
            }
            if (normalized.startsWith("exp") || normalized.contains("despesa")) {
                return TransactionType.EXPENSE;
            }
        }

        return TransactionType.EXPENSE;
    }

    private ParsedCsvRow parseCsvRow(String[] columns) {
        if (columns == null) {
            return null;
        }

        if (columns.length >= 5) {
            return new ParsedCsvRow(
                    columns[0].trim(),
                    columns[1].trim(),
                    columns[2].trim(),
                    columns[3].trim(),
                    columns[4].trim()
            );
        }

        if (columns.length >= 3) {
            return new ParsedCsvRow(
                    columns[0].trim(),
                    columns[1].trim(),
                    null,
                    columns[2].trim(),
                    null
            );
        }

        return null;
    }

    private String suggestCategoryWithAI(String description, BigDecimal amount, LocalDate date) {
        String prompt = "Sugira a categoria mais adequada para a seguinte transação bancária: " +
                "Descrição: '" + description + "', Valor: '" + amount + "', Data: '" + date + "'. " +
                "Responda apenas com o nome da categoria, sem explicações.";
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return result != null ? result.trim() : "Outros";
        } catch (Exception e) {
            LOGGER.warn("Falha ao sugerir categoria via IA, usando 'Outros'", e);
            return "Outros";
        }
    }

    private Category findOrCreateCategory(String categoryName, Long userId) {
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        return userCategories.stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName(categoryName);
                    newCat.setUser(importFileRepository.findByUserId(userId).stream().findFirst()
                            .map(ImportFile::getUser).orElse(null));
                    newCat.setType(org.tech_solutions.application.categories.enums.CategoryType.EXPENSE); // padrão
                    newCat.setCreatedAt(LocalDateTime.now());
                    return categoryRepository.save(newCat);
                });
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
            String normalized = value.trim().replace("R$", "");
            if (normalized.contains(",") && normalized.contains(".")) {
                normalized = normalized.replace(".", "").replace(",", ".");
            } else if (normalized.contains(",")) {
                normalized = normalized.replace(",", ".");
            }
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

    private record ParsedCsvRow(
            String dateValue,
            String descriptionValue,
            String categoryValue,
            String amountValue,
            String typeValue) {
    }
}
