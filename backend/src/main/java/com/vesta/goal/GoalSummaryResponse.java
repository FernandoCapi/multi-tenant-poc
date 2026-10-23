package com.vesta.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One line of the goal list, from the point of view of the current user. */
public record GoalSummaryResponse(
        UUID id,
        String title,
        GoalStatus status,
        BigDecimal targetAmount,
        String currency,
        LocalDate dueDate,
        GoalRole myRole,
        boolean iContribute) {

    static GoalSummaryResponse of(Goal goal, GoalMember membership) {
        return new GoalSummaryResponse(goal.getId(), goal.getTitle(), goal.getStatus(), goal.getTargetAmount(),
                goal.getCurrency(), goal.getDueDate(), membership.getRole(), membership.isContributes());
    }
}
