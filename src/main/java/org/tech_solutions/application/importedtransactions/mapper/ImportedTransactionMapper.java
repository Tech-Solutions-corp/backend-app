package org.tech_solutions.application.importedtransactions.mapper;

import org.tech_solutions.application.importedtransactions.dto.ImportedTransactionDataDTO;
import org.tech_solutions.application.importedtransactions.dto.ImportedTransactionRequestDTO;
import org.tech_solutions.application.importedtransactions.model.ImportedTransaction;

import java.util.List;

public class ImportedTransactionMapper {

    private ImportedTransactionMapper() {
    }

    public static ImportedTransaction toModel(ImportedTransactionRequestDTO dto) {
        ImportedTransaction transaction = new ImportedTransaction();
        transaction.setRawDescription(dto.rawDescription());
        transaction.setRawAmount(dto.rawAmount());
        transaction.setRawDate(dto.rawDate());
        transaction.setProcessed(dto.processed());
        return transaction;
    }

    public static ImportedTransactionDataDTO toDTO(ImportedTransaction transaction) {
        return new ImportedTransactionDataDTO(
                transaction.getId(),
                transaction.getImportFile().getId(),
                transaction.getRawDescription(),
                transaction.getRawAmount(),
                transaction.getRawDate(),
                transaction.getProcessed()
        );
    }

    public static List<ImportedTransactionDataDTO> toDTO(List<ImportedTransaction> transactions) {
        return transactions.stream().map(ImportedTransactionMapper::toDTO).toList();
    }
}

