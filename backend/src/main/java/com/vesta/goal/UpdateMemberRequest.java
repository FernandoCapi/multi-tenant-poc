package com.vesta.goal;

import jakarta.validation.constraints.AssertTrue;

/** Partial update of a membership: role (access) and/or contributes (financial participation). */
public record UpdateMemberRequest(GoalRole role, Boolean contributes) {

    @AssertTrue
    public boolean isAnyFieldPresent() {
        return role != null || contributes != null;
    }
}
