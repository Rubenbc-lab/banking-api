package com.banking.controller;


import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionDTO;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawalRequest;
import com.banking.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService =transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionDTO> deposit(@Valid @RequestBody DepositRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.deposit(request,ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/deposit")
    public ResponseEntity<TransactionDTO> adminDeposit(@Valid @RequestBody DepositRequest request) {
        TransactionDTO result = transactionService.adminDeposit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionDTO> withdraw(@Valid @RequestBody WithdrawalRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.withdraw(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PostMapping("/admin/withdraw")
    public ResponseEntity<TransactionDTO> adminWithdraw(@Valid @RequestBody WithdrawalRequest request) {
        TransactionDTO result = transactionService.adminWithdraw(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();
        TransactionDTO result = transactionService.transferMoney(ownerEmail,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/movements/{iban}")
    public ResponseEntity<List<TransactionDTO>> viewTransactions(@PathVariable("iban") String iban, Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.OK).body(transactionService.getTransactionsForAccount(iban,email));
    }
    @GetMapping("/admin/movements/{iban}")
    public ResponseEntity<List<TransactionDTO>> getAccountTransactionsForAdmin(@PathVariable("iban") String iban) {
        return ResponseEntity.ok(transactionService.adminGetTransactionsByIban(iban));
    }
}

