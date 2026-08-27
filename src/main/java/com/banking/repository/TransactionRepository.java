package com.banking.repository;

import com.banking.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceIbanOrTargetIbanOrderByTimestampDesc(
            String sourceIban,
            String targetIban
    );
}
