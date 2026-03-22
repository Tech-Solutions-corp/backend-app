package org.tech_solutions.application.transactions.mapper;

import org.tech_solutions.application.transactions.dto.TransactionDataDTO;
import org.tech_solutions.application.transactions.dto.TransactionRequestDTO;
import org.tech_solutions.application.transactions.model.Transaction;

import java.util.List;

public class TransactionMapper {

    private TransactionMapper() {
    }

    public static Transaction toModel(TransactionRequestDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDescription(dto.transactionDescription());
        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.transactionDate());
        transaction.setTransactionType(dto.transactionType());
        return transaction;
    }

    public static TransactionDataDTO toDTO(Transaction transaction) {
        return new TransactionDataDTO(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getAccount().getId(),
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getTransactionDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                transaction.getCreatedAt()
        );
    }

    public static List<TransactionDataDTO> toDTO(List<Transaction> transactions) {
        return transactions.stream().map(TransactionMapper::toDTO).toList();
    }
}

