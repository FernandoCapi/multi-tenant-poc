package com.vesta.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoalDetailResponse(
        UUID id,
        String title,
        String description,
        GoalStatus status,
        BigDecimal targetAmount,
        String currency,
        LocalDate dueDate,
        UUID createdBy,
        Instant createdAt,
        Instant completedAt,
        Instant archivedAt,
        GoalRole myRole,
        boolean iContribute,
        List<MemberResponse> members) {

    public record MemberResponse(UUID userId, String displayName, GoalRole role, boolean contributes) {
    }
}
