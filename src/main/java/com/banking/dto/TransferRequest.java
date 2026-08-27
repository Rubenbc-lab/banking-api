package com.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest (
    @NotBlank(message = "Source IBAN is required")
    String sourceIban,

    @NotBlank(message = "Target IBAN is required")
    String targetIban,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    BigDecimal amount

)
{
}
