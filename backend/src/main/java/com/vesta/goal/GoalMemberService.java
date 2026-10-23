package com.vesta.goal;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vesta.auth.CurrentUser;
import com.vesta.web.RequestValidator;

/**
 * Membership management (D-01, D-05..D-07). Every change locks the goal row first, so two owners
 * leaving at the same time cannot leave the goal without an owner (INV-04).
 */
@Service
public class GoalMemberService {

    private static final String LAST_OWNER_MESSAGE = "O objetivo precisa de pelo menos um dono.";

    private final GoalMemberRepository goalMemberRepository;
    private final GoalAccessService accessService;
    private final GoalService goalService;
    private final RequestValidator requestValidator;
    private final Clock clock;

    public GoalMemberService(GoalMemberRepository goalMemberRepository, GoalAccessService accessService,
            GoalService goalService, RequestValidator requestValidator, Clock clock) {
        this.goalMemberRepository = goalMemberRepository;
        this.accessService = accessService;
        this.goalService = goalService;
        this.requestValidator = requestValidator;
        this.clock = clock;
    }

    @Transactional
    public GoalDetailResponse add(UUID goalId, NewMemberRequest request) {
        GoalAccess access = accessService.requireOwner(accessService.requireVisibleForUpdate(goalId));
        requestValidator.validate(request);
        goalService.requireFamilyMember(request.userId());
        if (goalMemberRepository.existsById(new GoalMemberId(goalId, request.userId()))) {
            throw new GoalStateConflictException("Esta pessoa já é membro do objetivo.");
        }
        goalMemberRepository.save(new GoalMember(goalId, request.userId(), request.role(),
                request.contributesOrDefault(), CurrentUser.id(), clock.instant()));
        return goalService.toDetail(access);
    }

    @Transactional
    public GoalDetailResponse update(UUID goalId, UUID userId, UpdateMemberRequest request) {
        GoalAccess access = accessService.requireOwner(accessService.requireVisibleForUpdate(goalId));
        requestValidator.validate(request);
        GoalMember member = membership(goalId, userId);
        if (request.role() == GoalRole.MEMBER && member.isOwner()) {
            requireAnotherOwner(goalId);
        }
        if (request.role() != null) {
            member.changeRole(request.role());
        }
        if (request.contributes() != null) {
            member.changeContributes(request.contributes());
        }
        return goalService.toDetail(access);
    }

    /**
     * Owners remove anyone; any member may remove themselves (leave). The contributions of whoever
     * leaves stay in the history and in the totals (D-07).
     */
    @Transactional
    public void remove(UUID goalId, UUID userId) {
        GoalAccess access = accessService.requireVisibleForUpdate(goalId);
        if (!userId.equals(CurrentUser.id())) {
            accessService.requireOwner(access);
        }
        GoalMember member = membership(goalId, userId);
        if (member.isOwner()) {
            requireAnotherOwner(goalId);
        }
        goalMemberRepository.delete(member);
    }

    private GoalMember membership(UUID goalId, UUID userId) {
        return goalMemberRepository.findById(new GoalMemberId(goalId, userId))
                .orElseThrow(GoalMemberNotFoundException::new);
    }

    private void requireAnotherOwner(UUID goalId) {
        if (goalMemberRepository.countByGoalIdAndRole(goalId, GoalRole.OWNER) <= 1) {
            throw new GoalStateConflictException(LAST_OWNER_MESSAGE);
        }
    }
}
