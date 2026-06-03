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
            LOGGER.info("Nenhuma transacao pendente para reprocessar na importacao {}", importId);
            return 0;
        }

        List<ParsedCsvRow> csvRows = readCsvRowsFromMinio(importFile);
        if (csvRows.isEmpty()) {
            throw new UploadOperationException("Nao foi possivel ler o CSV da importacao para reprocessar");
        }

        int total = Math.min(pendingTransactions.size(), csvRows.size());
        int processedCount = 0;
        int failedCount = 0;
        List<ImportedTransaction> processedTransactions = new ArrayList<>();

        for (int index = 0; index < total; index++) {
            try {
                ImportedTransaction importedTransaction = pendingTransactions.get(index);
                ParsedCsvRow csvRow = csvRows.get(index);

                Account account = importedTransaction.getAccount();
                if (account == null) {
                    LOGGER.warn("Transacao {}: conta nao encontrada", importedTransaction.getId());
                    failedCount++;
                    continue;
                }

                LocalDate rawDate = parseDate(csvRow.dateValue());
                BigDecimal rawAmount = parseAmount(csvRow.amountValue());
                if (rawDate == null || rawAmount == null) {
                    LOGGER.warn("Transacao {}: data ou valor invalido", importedTransaction.getId());
                    failedCount++;
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

                if (category == null) {
                    LOGGER.warn("Transacao {}: nao foi possivel associar categoria", importedTransaction.getId());
                    failedCount++;
                    continue;
                }

                Transaction transaction = new Transaction();
                transaction.setTransactionDescription(description);
                transaction.setAmount(rawAmount);
                transaction.setTransactionDate(rawDate);
                transaction.setTransactionType(transactionType);
                transactionService.create(transaction, importFile.getUser().getId(), account.getId(), category.getId());

                importedTransaction.setProcessed(true);
                processedTransactions.add(importedTransaction);
                processedCount++;

                LOGGER.debug("Transacao {} reprocessada com sucesso", importedTransaction.getId());
            } catch (Exception e) {
                LOGGER.error("Erro ao reprocessar transacao no indice {}", index, e);
                failedCount++;
            }
        }

        if (processedCount > 0) {
            importedTransactionRepository.saveAll(processedTransactions);
            importFile.setStatus(ImportStatus.COMPLETED);
            importFileRepository.save(importFile);
            LOGGER.info("Reprocessamento concluido: {} sucesso, {} falhas", processedCount, failedCount);
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
                    ParsedCsvRow csvRow = parseCsvRow(columns);
                    if (csvRow != null) {
                        String desc = "%s | %s | R$%s".formatted(
                                csvRow.dateValue() == null ? "" : csvRow.dateValue(),
                                csvRow.descriptionValue() == null ? "" : csvRow.descriptionValue(),
                                csvRow.amountValue() == null ? "" : csvRow.amountValue()
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
        int lineNumber = 1; // começa em 1 (descontando header)

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                LOGGER.warn("Arquivo CSV vazio");
                return;
            }

            LOGGER.info("Header do CSV: {}", header);
            String line;
            int processedLines = 0;
            int skippedLines = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                String[] columns = parseCsvLine(line);
                if (columns.length < 3) {
                    LOGGER.debug("Linha {} ignorada: insuficientes colunas ({})", lineNumber, columns.length);
                    skippedLines++;
                    continue;
                }

                ParsedCsvRow parsedRow = parseCsvRow(columns);
                if (parsedRow == null) {
                    LOGGER.debug("Linha {} ignorada: falha no parsing padrão", lineNumber);
                    skippedLines++;
                    continue;
                }

                LocalDate rawDate = parseDate(parsedRow.dateValue());
                BigDecimal rawAmount = parseAmount(parsedRow.amountValue());

                if (rawDate == null) {
                    LOGGER.warn("Linha {}: data invalida '{}'", lineNumber, parsedRow.dateValue());
                    skippedLines++;
                    continue;
                }

                if (rawAmount == null) {
                    LOGGER.warn("Linha {}: valor invalido '{}'", lineNumber, parsedRow.amountValue());
                    skippedLines++;
                    continue;
                }

                rawAmount = rawAmount.abs();

                String description = parsedRow.descriptionValue();
                String suggestedCategoryName = resolveCategoryName(parsedRow.categoryValue(), description, rawAmount, rawDate);
                Category category = findOrCreateCategory(suggestedCategoryName, userId);

                if (category == null) {
                    LOGGER.warn("Linha {}: nao foi possivel associar categoria '{}'", lineNumber, suggestedCategoryName);
                    skippedLines++;
                    continue;
                }

                TransactionType transactionType = parseTransactionType(parsedRow.typeValue(), rawAmount);

                try {
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
                    processedLines++;

                    LOGGER.debug("Linha {} processada com sucesso", lineNumber);
                } catch (Exception e) {
                    LOGGER.error("Linha {}: erro ao criar transacao", lineNumber, e);
                    skippedLines++;
                }
            }

            LOGGER.info("CSV processado: {} linhas processadas, {} ignoradas", processedLines, skippedLines);

        } catch (IOException e) {
            throw new UploadOperationException("Nao foi possivel processar o CSV importado");
        }

        if (!importedTransactions.isEmpty()) {
            importedTransactionRepository.saveAll(importedTransactions);
            LOGGER.info("Importadas {} transacoes no banco de dados", importedTransactions.size());
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

        // Trim all columns once
        String[] cols = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            cols[i] = columns[i] == null ? null : columns[i].trim();
        }

        // Heurística: localizar índice do valor (amount) e da data, se possível
        Integer amountIdx = null;
        for (int i = 0; i < cols.length; i++) {
            if (cols[i] == null || cols[i].isBlank()) continue;
            if (parseAmount(cols[i]) != null) {
                // preferir o último candidato (muitos extratos colocam o valor ao final)
                amountIdx = i;
            }
        }

        Integer dateIdx = null;
        for (int i = 0; i < cols.length; i++) {
            if (cols[i] == null || cols[i].isBlank()) continue;
            if (parseDate(cols[i]) != null) {
                dateIdx = i;
                break; // prefere a primeira data encontrada
            }
        }

        if (amountIdx != null && dateIdx != null && !amountIdx.equals(dateIdx)) {
            // tenta extrair descrição como coluna entre data e valor, ou a primeira coluna disponível
            int min = Math.min(amountIdx, dateIdx);
            int max = Math.max(amountIdx, dateIdx);

            String description = null;
            for (int i = min + 1; i < max; i++) {
                if (i >= 0 && i < cols.length && cols[i] != null && !cols[i].isBlank()) {
                    description = cols[i];
                    break;
                }
            }
            if (description == null) {
                for (int i = 0; i < cols.length; i++) {
                    if (i != amountIdx && i != dateIdx && cols[i] != null && !cols[i].isBlank()) {
                        description = cols[i];
                        break;
                    }
                }
            }

            // tenta categoria entre data e valor (se existir e diferente da descrição)
            String category = null;
            for (int i = min + 1; i < max; i++) {
                if (i >= 0 && i < cols.length && cols[i] != null && !cols[i].isBlank() && !cols[i].equals(description)) {
                    category = cols[i];
                    break;
                }
            }

            // tenta tipo como coluna imediatamente após o valor
            String type = null;
            if (amountIdx + 1 < cols.length) {
                type = cols[amountIdx + 1];
            }

            return new ParsedCsvRow(cols[dateIdx], description, category, cols[amountIdx], type);
        }

        // Fallbacks: formatos esperados previamente
        if (cols.length >= 5) {
            return new ParsedCsvRow(
                    cols[0],
                    cols[1],
                    cols[2],
                    cols[3],
                    cols[4]
            );
        }

        if (cols.length >= 3) {
            return new ParsedCsvRow(
                    cols[0],
                    cols[1],
                    null,
                    cols[2],
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
        String finalCategoryName;

        if (categoryName == null || categoryName.isBlank()) {
            finalCategoryName = "Outros";
        } else {
            finalCategoryName = categoryName.trim();
        }

        // Proteções: se a categoria parecer um UUID, número ou for muito longa, use 'Outros'
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        if (finalCategoryName.matches(uuidPattern) || finalCategoryName.matches("\\\\d+") || finalCategoryName.length() > 100) {
            LOGGER.warn("Categoria detectada como inválida (uuid/numero/tamanho): {} - usando 'Outros' em seu lugar", finalCategoryName);
            finalCategoryName = "Outros";
        }

        // 'searchName' é efetivamente final e pode ser capturado por lambdas/closures
        final String searchName = finalCategoryName;

        try {
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            return userCategories.stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(searchName))
                    .findFirst()
                    .orElseGet(() -> {
                        try {
                            Category newCat = new Category();
                            newCat.setName(searchName);

                            // Usa o usuário do arquivo de importação diretamente
                            newCat.setUser(importFileRepository.findByUserId(userId).stream()
                                    .findFirst()
                                    .map(ImportFile::getUser)
                                    .orElse(null));

                            if (newCat.getUser() == null) {
                                LOGGER.warn("Usuario nao encontrado para categoria {}", searchName);
                                return null;
                            }

                            newCat.setType(org.tech_solutions.application.categories.enums.CategoryType.EXPENSE);
                            newCat.setCreatedAt(LocalDateTime.now());
                            return categoryRepository.save(newCat);
                        } catch (Exception e) {
                            LOGGER.error("Erro ao criar categoria {}", searchName, e);
                            return null;
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar ou criar categoria {}", finalCategoryName, e);
            return null;
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
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        // Tenta ISO 8601 format (yyyy-MM-dd)
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored1) {
            // Ignora
        }

        // Tenta formato brasileiro (dd/MM/yyyy)
        try {
            String[] parts = value.split("/");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);

                // Corrige ano 2 dígitos
                if (year < 100) {
                    year += 2000;
                }

                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored2) {
            // Ignora
        }

        // Tenta formato americano (MM/dd/yyyy)
        try {
            String[] parts = value.split("/");
            if (parts.length == 3) {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);

                if (year < 100) {
                    year += 2000;
                }

                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored3) {
            // Ignora
        }

        LOGGER.warn("Nao foi possivel fazer parse da data: {}", value);
        return null;
    }

    private BigDecimal parseAmount(String value) {
        try {
            if (value == null) return null;
            String s = value.trim();

            boolean negative = false;
            // trata valores entre parênteses como negativos: (1.234,56)
            if (s.startsWith("(") && s.endsWith(")")) {
                negative = true;
                s = s.substring(1, s.length() - 1).trim();
            }

            // remove símbolos de moeda e espaços, mantendo dígitos, ponto, vírgula e sinal
            s = s.replaceAll("[^0-9,.\\-]", "");
            if (s.isEmpty()) return null;

            if (s.contains(",") && s.contains(".")) {
                // decide se formato é 1.234,56 (com ponto milhares e vírgula decimais)
                if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                    s = s.replace(".", "").replace(",", ".");
                } else {
                    // formato 1,234.56 -> remove vírgulas
                    s = s.replace(",", "");
                }
            } else if (s.contains(",")) {
                s = s.replace(",", ".");
            }

            BigDecimal result = new BigDecimal(s);
            return negative ? result.negate() : result;
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
