package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionDTO;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawalRequest;
import com.banking.entity.Account;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.transaction.Transaction;
import com.banking.util.TransactionType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this. accountRepository = accountRepository;
    }


    public TransactionDTO transferMoney(String ownerEmail, TransferRequest request) {
        if (request.sourceIban().equals(request.targetIban())) {
            throw new IllegalArgumentException("Source and target IBAN cannot be the same");
        }

        Account firstLock, secondLock;
        if (request.sourceIban().compareTo(request.targetIban()) < 0) {
            firstLock = accountRepository.findByIbanWithLock(request.sourceIban())
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
            secondLock = accountRepository.findByIbanWithLock(request.targetIban())
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
        } else {
            secondLock = accountRepository.findByIbanWithLock(request.targetIban())
                    .orElseThrow(() -> new NoSuchElementException("Target account not found"));
            firstLock = accountRepository.findByIbanWithLock(request.sourceIban())
                    .orElseThrow(() -> new NoSuchElementException("Source account not found"));
        }

        Account source = request.sourceIban().compareTo(request.targetIban()) < 0 ? firstLock : secondLock;
        Account target = request.sourceIban().compareTo(request.targetIban()) < 0 ? secondLock : firstLock;

        if (!source.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You are not the owner of the source account");
        }

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the transfer");
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));

        Transaction transaction = new Transaction(
                source.getIban(),
                target.getIban(),
                request.amount(),
                TransactionType.TRANSFER
        );
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }
    public TransactionDTO deposit(DepositRequest request, String ownerEmail) {
        Account target = accountRepository.findByIbanWithLock(request.targetIban())
                .orElseThrow(() -> new NoSuchElementException("Target account cannot be found"));
        if (!target.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You can only deposit money into your own accounts");
        }

        target.setBalance(target.getBalance().add(request.amount()));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                request.amount()
        );
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }
    public TransactionDTO adminDeposit(DepositRequest request) {
        Account target = accountRepository.findByIbanWithLock(request.targetIban())
                .orElseThrow(() -> new NoSuchElementException("Target account cannot be found"));
        target.setBalance(target.getBalance().add(request.amount()));
        Transaction transaction = Transaction.createDeposit(
                target.getIban(),
                request.amount()
        );
        Transaction saved = transactionRepository.save(transaction);
        return mapToDTO(saved);
    }

    public TransactionDTO withdraw(String ownerEmail, WithdrawalRequest request) {
        Account source = accountRepository.findByIbanWithLock(request.sourceIban())
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));
        if (!source.getOwner().equals(ownerEmail)) {
            throw new AccessDeniedException("You are not the owner of the source account");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(request.amount()));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                request.amount()
        );
        Transaction saved = transactionRepository.save(transaction);

        return mapToDTO(saved);
    }
    public TransactionDTO adminWithdraw(WithdrawalRequest request) {
        Account source = accountRepository.findByIbanWithLock(request.sourceIban())
                .orElseThrow(() -> new NoSuchElementException("Source account cannot be found"));

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance to complete the operation");
        }
        source.setBalance(source.getBalance().subtract(request.amount()));

        Transaction transaction = Transaction.createWithdrawal(
                source.getIban(),
                request.amount()
        );
        Transaction saved = transactionRepository.save(transaction);

        return mapToDTO(saved);
    }
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsForAccount(String iban, String authenticatedEmail) {
        Account account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementException("Account cannot be found"));
        if (!account.getOwner().equals(authenticatedEmail)) {
            throw new AccessDeniedException("You are not authorized to view transactions for this account");
        }
        return transactionRepository.findBySourceIbanOrTargetIbanOrderByTimestampDesc(iban,iban)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<TransactionDTO> adminGetTransactionsByIban(String iban) {
        if (!accountRepository.existsByIban(iban)) {
            throw new NoSuchElementException("Account cannot be found with IBAN: " + iban);
        }

        return transactionRepository.findBySourceIbanOrTargetIbanOrderByTimestampDesc(iban, iban)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getSourceIban(),
                transaction.getTargetIban(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTimestamp()
        );
    }
}
