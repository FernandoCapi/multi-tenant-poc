package com.vesta.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The family is never part of the request: the goal is created in the token's family (RS-04). */
public record CreateGoalRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,
        LocalDate dueDate,
        List<@Valid @NotNull NewMemberRequest> members) {
}
