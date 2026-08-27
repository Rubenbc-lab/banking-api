package com.banking;

import com.banking.dto.AccountDTO;
import com.banking.dto.AdminAccountCreationRequest;
import com.banking.jwt.JWTUtil;
import com.banking.repository.AccountRepository;
import com.banking.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountIntegrationTest extends AbstractTestContainers {
    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    private RestClient restClient;


    @Autowired
    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Must create a new account")
    void shouldCreateAnAccount() {
        String email = "test@gmail.com";
        String token = jwtUtil.generateTestToken(email, List.of("ROLE_USER"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<AccountDTO> response = restClient.post()
                .uri("/api/v1/accounts/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(AccountDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().owner()).isEqualTo(email);
        assertThat(response.getBody().iban()).startsWith("ES");
        assertThat(response.getBody().balance()).isEqualByComparingTo("0.0");

    }

    @Test
    @DisplayName("Must create an account in admin mode")
    void createAnAccountForUser() {
        String adminEmail = "admin@gmail.com";
        String token = jwtUtil.generateTestToken(adminEmail, List.of("ROLE_ADMIN"));

        String targetEmail = "test@gmail.com";
        BigDecimal amount = BigDecimal.valueOf(100);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        AdminAccountCreationRequest bodyRequest = new AdminAccountCreationRequest(targetEmail,amount);

        ResponseEntity<AccountDTO> response = restClient.post()
                .uri("/api/v1/accounts/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyRequest)
                .retrieve()
                .toEntity(AccountDTO.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().owner()).isEqualTo(targetEmail);
        assertThat(response.getBody().balance()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Must retrieve all customers' accounts")
    void getAllAccountsFromCustomer() {
        String ownerEmail = "test@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));



        HttpHeaders header = new HttpHeaders();
        header.setBearerAuth(token);
        header.setContentType(MediaType.APPLICATION_JSON);

        String otherEmail = "otheruser@gmail.com";

        accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));
        accountService.createAccount(ownerEmail, BigDecimal.valueOf(250));
        accountService.createAccount(otherEmail, BigDecimal.valueOf(500));


        ResponseEntity<List<AccountDTO>> response = restClient.get()
                .uri("/api/v1/accounts/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().toEntity(new ParameterizedTypeReference<List<AccountDTO>>() {
                });

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();

        List<AccountDTO> userAccounts = response.getBody();
        assertThat(userAccounts).hasSize(2);
    }

    @Test
    @DisplayName("Admin must retrieve all the created accounts")
    void getAllAccounts() {
        String adminEmail = "admin@gmail.com";
        String token = jwtUtil.generateTestToken(adminEmail, List.of("ROLE_ADMIN"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String otherEmail = "otheruser@gmail.com";
        String anotherEmail = "anotheruser@gmail.com";

        accountService.createAccount(adminEmail, BigDecimal.valueOf(100));
        accountService.createAccount(adminEmail, BigDecimal.valueOf(250));
        accountService.createAccount(otherEmail, BigDecimal.valueOf(500));
        accountService.createAccount(otherEmail, BigDecimal.valueOf(800));
        accountService.createAccount(anotherEmail, BigDecimal.valueOf(500));

        ResponseEntity<List<AccountDTO>> response = restClient.get()
                .uri("/api/v1/accounts/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().toEntity(new ParameterizedTypeReference<List<AccountDTO>>() {
                });

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();

        List<AccountDTO> userAccounts = response.getBody();
        assertThat(userAccounts).hasSize(5);
    }

    @Test
    @DisplayName("Customer can delete their own account by IBAN")
    void customerCanDeleteOwnAccount() {
        String ownerEmail = "customer@gmail.com";
        String otherEmail = "other@gmail.com";
        String token = jwtUtil.generateTestToken(ownerEmail, List.of("ROLE_USER"));

        AccountDTO accountToDelete = accountService.createAccount(ownerEmail, BigDecimal.valueOf(100));
        AccountDTO accountToKeep = accountService.createAccount(ownerEmail, BigDecimal.valueOf(250));
        AccountDTO otherAccount = accountService.createAccount(otherEmail, BigDecimal.valueOf(500));

        ResponseEntity<Void> response = restClient.delete()
                .uri("/api/v1/accounts/{iban}", accountToDelete.iban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(204);

        List<AccountDTO> remainingAccounts = accountService.getAccountsByOwner(ownerEmail);
        assertThat(remainingAccounts)
                .hasSize(1)
                .extracting(AccountDTO::iban)
                .containsExactly(accountToKeep.iban())
                .doesNotContain(accountToDelete.iban());

        List<AccountDTO> otherUserAccounts = accountService.getAccountsByOwner(otherEmail);
        assertThat(otherUserAccounts).hasSize(1);
    }

    @Test
    @DisplayName("Customer can delete their own account by IBAN")
    void adminCanDeleteAccounts() {
        String adminEmail = "admin@gmail.com";
        String otherEmail = "other@gmail.com";
        String anotherEmail = "another@gmail.com";
        String token = jwtUtil.generateTestToken(adminEmail, List.of("ROLE_ADMIN"));

        AccountDTO accountToDelete = accountService.createAccount(otherEmail, BigDecimal.valueOf(100));
        AccountDTO otherAccount = accountService.createAccount(anotherEmail, BigDecimal.valueOf(250));
        AccountDTO accountToKeep = accountService.createAccount(otherEmail, BigDecimal.valueOf(500));

        ResponseEntity<Void> response = restClient.delete()
                .uri("/api/v1/accounts/admin/{iban}", accountToDelete.iban())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(204);

        List<AccountDTO> remainingAccounts = accountService.getAccountsByOwner(otherEmail);
        assertThat(remainingAccounts)
                .hasSize(1)
                .extracting(AccountDTO::iban)
                .containsExactly(accountToKeep.iban())
                .doesNotContain(accountToDelete.iban());

        List<AccountDTO> otherUserAccounts = accountService.getAccountsByOwner(otherEmail);
        assertThat(otherUserAccounts).hasSize(1);
    }
}
