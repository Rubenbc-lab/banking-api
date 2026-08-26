package com.banking.service;

import com.banking.dto.AccountDTO;
import com.banking.entity.Account;
import com.banking.repository.AccountRepository;
import com.banking.util.IbanGenerator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AccountService {

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public AccountDTO createAccount(String ownerEmail, BigDecimal balance) {
        String generatedIban = IbanGenerator.generateSpanishIban();

        Account account = new Account(
                generatedIban,
                balance,
                ownerEmail
        );
        Account saved = repo.save(account);
        return new AccountDTO(
                saved.getIban(),
                saved.getBalance(),
                saved.getOwner()
        );
    }

    public List<AccountDTO> getAccountsByOwner(String owner) {
        return repo.findAllByOwner(owner).stream()
                .map(account -> new AccountDTO(
                        account.getIban(),
                        account.getBalance(),
                        account.getOwner()
                )).toList();
    }

    public List<AccountDTO> getAllAccounts() {
        return repo.findAll().stream()
                .map(account ->  new AccountDTO(
                        account.getIban(),
                        account.getBalance(),
                        account.getOwner()
                )).toList();
    }
    public void deleteAccount(String owner,String iban) {
        Account account = repo.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("Account with IBAN [%s] not found".formatted(iban)));
        if (!account.getOwner().equals(owner)) {
            throw new AccessDeniedException("You do not have permission to delete this account");
        }
        repo.delete(account);
    }
    public void deleteAccountByAdmin(String iban) {
        Account account = repo.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("Account with IBAN [%s] not found".formatted(iban)));
        repo.delete(account);
    }
}
