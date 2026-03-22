package org.tech_solutions.application.accounts.mapper;

import org.tech_solutions.application.accounts.dto.AccountDataDTO;
import org.tech_solutions.application.accounts.dto.AccountRequestDTO;
import org.tech_solutions.application.accounts.model.Account;

import java.util.List;

public class AccountMapper {

    private AccountMapper() {
    }

    public static Account toModel(AccountRequestDTO dto) {
        Account account = new Account();
        account.setName(dto.name());
        account.setType(dto.type());
        account.setBalance(dto.balance());
        return account;
    }

    public static AccountDataDTO toDTO(Account account) {
        return new AccountDataDTO(
                account.getId(),
                account.getUser().getId(),
                account.getName(),
                account.getType(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public static List<AccountDataDTO> toDTO(List<Account> accounts) {
        return accounts.stream().map(AccountMapper::toDTO).toList();
    }
}

