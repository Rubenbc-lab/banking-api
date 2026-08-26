package com.banking.dto;

import java.math.BigDecimal;

public record AdminAccountCreationRequest(
        String ownerEmail,
        BigDecimal initialBalance
) {
}
