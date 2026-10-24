package com.vesta.goal;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Identifier of {@link GoalMember}: one membership per user and goal. */
public class GoalMemberId implements Serializable {

    private UUID goalId;
    private UUID userId;

    protected GoalMemberId() {
    }

    public GoalMemberId(UUID goalId, UUID userId) {
        this.goalId = goalId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GoalMemberId that
                && Objects.equals(goalId, that.goalId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goalId, userId);
    }
}
