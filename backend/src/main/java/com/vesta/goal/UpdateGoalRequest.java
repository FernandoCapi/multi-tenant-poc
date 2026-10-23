package com.vesta.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: absent fields keep their current value. */
public record UpdateGoalRequest(
        @Size(max = 120) @Pattern(regexp = ".*\\S.*") String title,
        @Size(max = 1000) String description,
        @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,
        LocalDate dueDate) {
}
