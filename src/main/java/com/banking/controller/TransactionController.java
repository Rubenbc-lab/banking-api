package com.banking.controller;


import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionDTO;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawalRequest;
import com.banking.repository.TransactionRepository;
import com.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private TransactionRepository repo;
    private TransactionService transactionService;

    public TransactionController(TransactionRepository repo, TransactionService transactionService) {
        this.repo = repo;
        this.transactionService =transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody DepositRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.deposit(request,ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@Valid @RequestBody WithdrawalRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.withdraw(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.transferMoney(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

