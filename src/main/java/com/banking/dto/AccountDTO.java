package com.banking.dto;

import java.math.BigDecimal;

public record AccountDTO(

        String iban,
        BigDecimal balance,
        String owner
) {}
