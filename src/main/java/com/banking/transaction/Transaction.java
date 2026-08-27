package com.banking.transaction;

import com.banking.util.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_iban")
    private String sourceIban;

    @Column(name = "target_iban")
    private String targetIban;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public Transaction() {}

    public Transaction(String sourceIban, String targetIban,BigDecimal amount, TransactionType type) {
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public static Transaction createTransfer(String sourceIban, String targetIban, BigDecimal amount) {
        return new Transaction(sourceIban, targetIban, amount, TransactionType.TRANSFER);
    }

    public static Transaction createDeposit(String targetIban, BigDecimal amount) {
        return new Transaction(null, targetIban, amount, TransactionType.DEPOSIT);
    }

    public static Transaction createWithdrawal(String sourceIban, BigDecimal amount) {
        return new Transaction(sourceIban, null, amount, TransactionType.WITHDRAWAL);
    }

    public Long getId() {
        return id;
    }

    public String getSourceIban() {
        return sourceIban;
    }

    public String getTargetIban() {
        return targetIban;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}
