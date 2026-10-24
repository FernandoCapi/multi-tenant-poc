package com.vesta.contribution;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A contribution as listed to members of its goal; {@code reversal} is null while it counts. */
public record ContributionResponse(
        UUID id,
        UUID goalId,
        Person contributor,
        Person registeredBy,
        BigDecimal amount,
        LocalDate contributedOn,
        String note,
        Instant createdAt,
        boolean reversed,
        Reversal reversal) {

    public record Person(UUID id, String displayName) {
    }

    public record Reversal(Person reversedBy, String reason, Instant reversedAt) {
    }
}
