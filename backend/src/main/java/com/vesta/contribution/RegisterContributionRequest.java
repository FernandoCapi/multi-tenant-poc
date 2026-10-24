package com.vesta.contribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code contributorId} absent means the current user's own contribution; only owners register on
 * behalf of another participant (D-03). {@code contributedOn} absent means today (D-18).
 */
public record RegisterContributionRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        LocalDate contributedOn,
        @Size(max = 500) String note,
        UUID contributorId) {
}
