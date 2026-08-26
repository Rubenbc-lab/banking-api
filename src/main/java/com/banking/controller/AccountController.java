package com.banking.controller;

import com.banking.dto.AccountDTO;
import com.banking.dto.AdminAccountCreationRequest;
import com.banking.repository.AccountRepository;
import com.banking.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository repo;
    private final AccountService accountService;

    public AccountController(AccountRepository repo, AccountService accountService) {
        this.repo = repo;
        this.accountService = accountService;
    }

    @PostMapping("/me")
    public ResponseEntity<AccountDTO> createAccount(Authentication authentication) {
        String ownerEmail = authentication.getName();
        BigDecimal balance = BigDecimal.ZERO;
        AccountDTO createdAccount = accountService.createAccount(ownerEmail, balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDTO> createAccountForCustomer(@RequestBody AdminAccountCreationRequest request) {
        String ownerEmail = request.ownerEmail();
        BigDecimal balance = request.initialBalance();
        AccountDTO createdAccount = accountService.createAccount(ownerEmail,balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);

    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountDTO>> getUserAccounts(Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(accountService.getAccountsByOwner(ownerEmail));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountDTO>> getAllUsersAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }
}
