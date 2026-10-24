package com.vesta.goal;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** {@code contributes} defaults to true when omitted (D-05). */
public record NewMemberRequest(@NotNull UUID userId, @NotNull GoalRole role, Boolean contributes) {

    public boolean contributesOrDefault() {
        return contributes == null || contributes;
    }
}
