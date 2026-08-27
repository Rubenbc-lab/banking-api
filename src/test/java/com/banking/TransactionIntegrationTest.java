package com.banking;

import com.banking.dto.*;
import com.banking.entity.Account;
import com.banking.jwt.JWTUtil;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.AccountService;
import com.banking.service.TransactionService;
import com.banking.transaction.Transaction;
import com.banking.util.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionIntegrationTest extends AbstractTestContainers {
    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;

    private RestClient restClient;

    @Autowired
    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        accountRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("Must  deposit valid amounts")
    void customerCanDepositValidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        AccountDTO account = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));

        DepositRequest depositRequest = new DepositRequest(account.iban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/deposit")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(depositRequest)
                .retrieve().toEntity(TransactionDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().targetIban()).isEqualTo(account.iban());
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.DEPOSIT);

        Account updatedAccount = accountRepository.findByIban(account.iban()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("Must reject invalid deposit amounts (negative or zero)")
    void customerCannotDepositInvalidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        AccountDTO account = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));

        DepositRequest depositRequest = new DepositRequest(account.iban(), BigDecimal.valueOf(-50));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/deposit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(depositRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.iban())
                        .orElseThrow(() -> new NoSuchElementException("Cannot found the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("Must reject invalid withdrawal amounts (negative or zero)")
    void customerCannotWithdrawInvalidAmount() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        AccountDTO account = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.iban(), BigDecimal.valueOf(-50));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(withdrawalRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.iban())
                .orElseThrow(() -> new NoSuchElementException("Cannot find the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(transactionRepository.count()).isZero();
    }
    @Test
    @DisplayName("Must reject withdrawal when balance is insufficient")
    void customerCannotWithdrawMoreThanCurrentBalance() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        AccountDTO account = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.iban(), BigDecimal.valueOf(150));

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(withdrawalRequest)
                        .retrieve()
                        .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedAccount = accountRepository.findByIban(account.iban())
                .orElseThrow(() -> new NoSuchElementException("Cannot find the account"));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
    @Test
    @DisplayName("Must withdraw a valid amount")
    void customerCanWithdrawValidAmounts() {
        String ownerEmail = "customer@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));
        AccountDTO account = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));

        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(account.iban(), BigDecimal.valueOf(50));

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/withdraw")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(withdrawalRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().sourceIban()).isEqualTo(account.iban());
        assertThat(response.getBody().targetIban()).isNull();
        assertThat(response.getBody().type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.getBody().timestamp()).isNotNull();

        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(1);

        Transaction savedTx = transactions.getFirst();
        assertThat(savedTx.getSourceIban()).isEqualTo(account.iban());
        assertThat(savedTx.getTargetIban()).isNull();
        assertThat(savedTx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(savedTx.getType()).isEqualTo(TransactionType.WITHDRAWAL);
    }
    @Test
    @DisplayName("Customer can transfer money to another account successfully")
    void customerCanTransferMoneySuccessfully() {
        String senderEmail = "sender@gmail.com";
        String receiverEmail = "receiver@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        AccountDTO sourceAccount = accountService.createAccount(senderEmail, BigDecimal.valueOf(100));
        AccountDTO targetAccount = accountService.createAccount(receiverEmail, BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.iban(),
                targetAccount.iban(),
                BigDecimal.valueOf(40)
        );

        ResponseEntity<TransactionDTO> response = restClient.post()
                .uri("/api/v1/transactions/transfer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(transferRequest)
                .retrieve()
                .toEntity(TransactionDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().sourceIban()).isEqualTo(sourceAccount.iban());
        assertThat(response.getBody().targetIban()).isEqualTo(targetAccount.iban());
        assertThat(response.getBody().amount()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.getBody().type()).isEqualTo(TransactionType.TRANSFER);

        Account updatedSource = accountRepository.findByIban(sourceAccount.iban()).orElseThrow();
        Account updatedTarget = accountRepository.findByIban(targetAccount.iban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(90));

        List<Transaction> transactions = transactionRepository.findAll();
        assertThat(transactions.size()).isEqualTo(1);
        assertThat(transactions.getFirst().getType()).isEqualTo(TransactionType.TRANSFER);
    }
    @Test
    @DisplayName("Must reject transfer when amount is negative or zero")
    void customerCannotTransferInvalidAmount() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        AccountDTO sourceAccount = accountService.createAccount(senderEmail, BigDecimal.valueOf(100));
        AccountDTO targetAccount = accountService.createAccount("receiver@gmail.com", BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.iban(),
                targetAccount.iban(),
                BigDecimal.valueOf(-10)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedSource = accountRepository.findByIban(sourceAccount.iban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(transactionRepository.count()).isZero();
    }
    @Test
    @DisplayName("Must reject transfer when sender has insufficient balance")
    void customerCannotTransferWithInsufficientBalance() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        AccountDTO sourceAccount = accountService.createAccount(senderEmail, BigDecimal.valueOf(30));
        AccountDTO targetAccount = accountService.createAccount("receiver@gmail.com", BigDecimal.valueOf(50));

        TransferRequest transferRequest = new TransferRequest(
                sourceAccount.iban(),
                targetAccount.iban(),
                BigDecimal.valueOf(100)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        Account updatedSource = accountRepository.findByIban(sourceAccount.iban()).orElseThrow();
        Account updatedTarget = accountRepository.findByIban(targetAccount.iban()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(updatedTarget.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(transactionRepository.count()).isZero();
    }
    @Test
    @DisplayName("Must reject transfer to the same source account")
    void customerCannotTransferToSameAccount() {
        String senderEmail = "sender@gmail.com";
        String token = jwtUtil.generateTestToken(senderEmail, List.of("ROLE_USER"));

        AccountDTO account = accountService.createAccount(senderEmail, BigDecimal.valueOf(100));

        TransferRequest transferRequest = new TransferRequest(
                account.iban(),
                account.iban(),
                BigDecimal.valueOf(20)
        );

        assertThatThrownBy(() ->
                restClient.post()
                        .uri("/api/v1/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(transferRequest)
                        .retrieve()
                        .toBodilessEntity()
        ).isInstanceOf(HttpClientErrorException.BadRequest.class);

        assertThat(transactionRepository.count()).isZero();
    }
}
