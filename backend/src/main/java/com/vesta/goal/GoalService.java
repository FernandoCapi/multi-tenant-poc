package com.vesta.goal;

import java.time.Clock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vesta.auth.CurrentUser;
import com.vesta.family.FamilyMemberNotFoundException;
import com.vesta.user.User;
import com.vesta.user.UserRepository;
import com.vesta.web.RequestValidator;

/** Goal use cases. Every method resolves visibility first, then permission, validation and state. */
@Service
public class GoalService {

    private static final Set<GoalStatus> DEFAULT_LISTED_STATUSES = EnumSet.of(GoalStatus.ACTIVE, GoalStatus.COMPLETED);

    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalAccessService accessService;
    private final GoalContributionQueries contributionQueries;
    private final UserRepository userRepository;
    private final RequestValidator requestValidator;
    private final Clock clock;

    public GoalService(GoalRepository goalRepository, GoalMemberRepository goalMemberRepository,
            GoalAccessService accessService, GoalContributionQueries contributionQueries,
            UserRepository userRepository, RequestValidator requestValidator, Clock clock) {
        this.goalRepository = goalRepository;
        this.goalMemberRepository = goalMemberRepository;
        this.accessService = accessService;
        this.contributionQueries = contributionQueries;
        this.userRepository = userRepository;
        this.requestValidator = requestValidator;
        this.clock = clock;
    }

    /** Only goals the user is linked to; archived ones only when asked for (D-15). */
    @Transactional(readOnly = true)
    public List<GoalSummaryResponse> list(GoalStatus status) {
        UUID userId = CurrentUser.id();
        Set<GoalStatus> statuses = status == null ? DEFAULT_LISTED_STATUSES : EnumSet.of(status);
        Map<UUID, GoalMember> myMemberships = goalMemberRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(GoalMember::getGoalId, Function.identity()));
        return goalRepository.findVisible(userId, statuses).stream()
                .map(goal -> GoalSummaryResponse.of(goal, myMemberships.get(goal.getId())))
                .toList();
    }

    @Transactional
    public GoalDetailResponse create(CreateGoalRequest request) {
        UUID creatorId = CurrentUser.id();
        List<NewMemberRequest> others = request.members() == null ? List.of() : request.members();
        requireDistinctOtherMembers(others, creatorId);
        others.forEach(member -> requireFamilyMember(member.userId()));

        Goal goal = goalRepository.save(new Goal(request.title(), request.description(), request.targetAmount(),
                request.dueDate(), creatorId, clock.instant()));
        // The creator owns and contributes to the goal (INV-06)
        goalMemberRepository.save(new GoalMember(goal.getId(), creatorId, GoalRole.OWNER, true, creatorId,
                clock.instant()));
        others.forEach(member -> goalMemberRepository.save(new GoalMember(goal.getId(), member.userId(),
                member.role(), member.contributesOrDefault(), creatorId, clock.instant())));
        return detail(goal.getId());
    }

    @Transactional(readOnly = true)
    public GoalDetailResponse detail(UUID goalId) {
        return toDetail(accessService.requireVisible(goalId));
    }

    @Transactional
    public GoalDetailResponse update(UUID goalId, UpdateGoalRequest request) {
        GoalAccess access = accessService.requireOwner(accessService.requireVisible(goalId));
        requestValidator.validate(request);
        access.goal().updateDetails(request.title(), request.description(), request.targetAmount(), request.dueDate());
        return toDetail(access);
    }

    @Transactional
    public GoalDetailResponse complete(UUID goalId) {
        GoalAccess access = ownerAccess(goalId);
        access.goal().complete(clock.instant());
        return toDetail(access);
    }

    @Transactional
    public GoalDetailResponse reopen(UUID goalId) {
        GoalAccess access = ownerAccess(goalId);
        access.goal().reopen();
        return toDetail(access);
    }

    @Transactional
    public GoalDetailResponse archive(UUID goalId) {
        GoalAccess access = ownerAccess(goalId);
        access.goal().archive(clock.instant());
        return toDetail(access);
    }

    @Transactional
    public GoalDetailResponse unarchive(UUID goalId) {
        GoalAccess access = ownerAccess(goalId);
        access.goal().unarchive();
        return toDetail(access);
    }

    /** Physical deletion only for goals that never had a contribution (D-08, INV-11). */
    @Transactional
    public void delete(UUID goalId) {
        GoalAccess access = ownerAccess(goalId);
        if (contributionQueries.hasContributions(goalId)) {
            throw new GoalStateConflictException(
                    "Objetivos que já receberam contribuições não podem ser excluídos. Arquive o objetivo.");
        }
        goalMemberRepository.deleteByGoalId(goalId);
        goalRepository.delete(access.goal());
    }

    private GoalAccess ownerAccess(UUID goalId) {
        return accessService.requireOwner(accessService.requireVisible(goalId));
    }

    private void requireDistinctOtherMembers(List<NewMemberRequest> members, UUID creatorId) {
        Set<UUID> seen = new HashSet<>();
        for (NewMemberRequest member : members) {
            if (member.userId().equals(creatorId) || !seen.add(member.userId())) {
                throw new GoalStateConflictException("Cada membro só pode ser adicionado uma vez ao objetivo.");
            }
        }
    }

    /** The lookup runs under the token's family, so a user of another family is simply not found. */
    void requireFamilyMember(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new FamilyMemberNotFoundException();
        }
    }

    GoalDetailResponse toDetail(GoalAccess access) {
        Goal goal = access.goal();
        List<GoalMember> members = goalMemberRepository.findByGoalIdOrderByAddedAt(goal.getId());
        Map<UUID, String> names = userRepository.findAllById(members.stream().map(GoalMember::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
        List<GoalDetailResponse.MemberResponse> memberResponses = members.stream()
                .map(member -> new GoalDetailResponse.MemberResponse(member.getUserId(),
                        names.get(member.getUserId()), member.getRole(), member.isContributes()))
                .toList();
        return new GoalDetailResponse(goal.getId(), goal.getTitle(), goal.getDescription(), goal.getStatus(),
                goal.getTargetAmount(), goal.getCurrency(), goal.getDueDate(), goal.getCreatedBy(),
                goal.getCreatedAt(), goal.getCompletedAt(), goal.getArchivedAt(), access.membership().getRole(),
                access.membership().isContributes(), memberResponses);
    }
}
