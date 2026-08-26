package com.banking.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountCreationRequest (
    @NotBlank(message = "Owner identifier is required")
    String currency
) {}
