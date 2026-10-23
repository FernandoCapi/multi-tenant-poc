package com.vesta.goal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vesta.auth.CurrentUser;

/**
 * Per-goal authorization (RS-02, D-09). Row-level security already restricts every query to the
 * token's family; on top of it, a goal exists for a user only through their own membership.
 */
@Component
public class GoalAccessService {

    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;

    public GoalAccessService(GoalRepository goalRepository, GoalMemberRepository goalMemberRepository) {
        this.goalRepository = goalRepository;
        this.goalMemberRepository = goalMemberRepository;
    }

    /** 404 when the goal is missing, in another family, or the current user has no membership. */
    public GoalAccess requireVisible(UUID goalId) {
        return access(goalRepository.findById(goalId), goalId);
    }

    /** Same as {@link #requireVisible} but locks the goal row until the transaction ends. */
    public GoalAccess requireVisibleForUpdate(UUID goalId) {
        return access(goalRepository.findByIdForUpdate(goalId), goalId);
    }

    /** 403 when the current user sees the goal but is not one of its owners. */
    public GoalAccess requireOwner(GoalAccess access) {
        if (!access.isOwner()) {
            throw new GoalPermissionDeniedException();
        }
        return access;
    }

    private GoalAccess access(Optional<Goal> goal, UUID goalId) {
        GoalMember membership = goalMemberRepository.findById(new GoalMemberId(goalId, CurrentUser.id()))
                .orElseThrow(GoalNotFoundException::new);
        return new GoalAccess(goal.orElseThrow(GoalNotFoundException::new), membership);
    }
}
