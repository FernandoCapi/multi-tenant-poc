package com.vesta.contribution;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vesta.auth.CurrentUser;
import com.vesta.goal.GoalAccess;
import com.vesta.goal.GoalAccessService;
import com.vesta.goal.GoalMember;
import com.vesta.goal.GoalMemberId;
import com.vesta.goal.GoalMemberRepository;
import com.vesta.goal.GoalPermissionDeniedException;
import com.vesta.goal.GoalStateConflictException;
import com.vesta.goal.GoalStatus;
import com.vesta.user.User;
import com.vesta.user.UserRepository;
import com.vesta.web.RequestValidator;

/**
 * Contribution use cases (RF-04). Order of checks: visibility (404), permission (403), validation
 * (422), state (409). Every write locks the goal row, so it is serialized with completing or
 * archiving the same goal.
 */
@Service
public class ContributionService {

    /** "Today" for the not-in-the-future rule (D-18). */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final String ALREADY_REVERSED_MESSAGE = "Esta contribuição já foi estornada.";

    private final ContributionRepository contributionRepository;
    private final ContributionReversalRepository reversalRepository;
    private final GoalAccessService accessService;
    private final GoalMemberRepository goalMemberRepository;
    private final UserRepository userRepository;
    private final RequestValidator requestValidator;
    private final Clock clock;

    public ContributionService(ContributionRepository contributionRepository,
            ContributionReversalRepository reversalRepository, GoalAccessService accessService,
            GoalMemberRepository goalMemberRepository, UserRepository userRepository,
            RequestValidator requestValidator, Clock clock) {
        this.contributionRepository = contributionRepository;
        this.reversalRepository = reversalRepository;
        this.accessService = accessService;
        this.goalMemberRepository = goalMemberRepository;
        this.userRepository = userRepository;
        this.requestValidator = requestValidator;
        this.clock = clock;
    }

    @Transactional
    public ContributionResponse register(UUID goalId, RegisterContributionRequest request) {
        UUID currentUser = CurrentUser.id();
        GoalAccess access = accessService.requireVisibleForUpdate(goalId);
        UUID contributorId = request.contributorId() == null ? currentUser : request.contributorId();

        if (contributorId.equals(currentUser)) {
            // Seeing the goal is not enough: only participants contribute (D-05, N-6)
            if (!access.membership().isContributes()) {
                throw new GoalPermissionDeniedException();
            }
        } else {
            // Registering on behalf of someone else is an owner action (D-03)
            accessService.requireOwner(access);
        }

        requestValidator.validate(request);
        LocalDate contributedOn = request.contributedOn() == null ? today() : request.contributedOn();
        if (contributedOn.isAfter(today())) {
            throw new InvalidContributionException("A data da contribuição não pode ser futura.");
        }
        if (!contributorId.equals(currentUser)) {
            requireParticipant(goalId, contributorId);
        }
        requireActive(access, "Só objetivos ativos recebem contribuições.");

        Contribution contribution = contributionRepository.save(new Contribution(goalId, contributorId,
                currentUser, request.amount(), contributedOn, request.note(), clock.instant()));
        return toResponses(List.of(contribution), Map.of()).getFirst();
    }

    /**
     * Owners reverse any contribution; members only those attributed to them (D-07). Only while the
     * goal is ACTIVE (D-17), and at most once (D-13).
     */
    @Transactional
    public ContributionResponse reverse(UUID goalId, UUID contributionId, ReverseContributionRequest request) {
        UUID currentUser = CurrentUser.id();
        GoalAccess access = accessService.requireVisibleForUpdate(goalId);
        Contribution contribution = contributionRepository.findByIdAndGoalId(contributionId, goalId)
                .orElseThrow(ContributionNotFoundException::new);
        if (!contribution.getContributorId().equals(currentUser)) {
            accessService.requireOwner(access);
        }

        requestValidator.validate(request);
        requireActive(access, "Só contribuições de objetivos ativos podem ser estornadas.");
        if (reversalRepository.existsByContributionId(contributionId)) {
            throw new GoalStateConflictException(ALREADY_REVERSED_MESSAGE);
        }

        ContributionReversal reversal;
        try {
            reversal = reversalRepository.saveAndFlush(
                    new ContributionReversal(contributionId, currentUser, request.reason(), clock.instant()));
        } catch (DataIntegrityViolationException ex) {
            // Concurrent reversal of the same contribution: the UNIQUE constraint decides (D-13)
            throw new GoalStateConflictException(ALREADY_REVERSED_MESSAGE);
        }
        return toResponses(List.of(contribution), Map.of(contributionId, reversal)).getFirst();
    }

    private void requireParticipant(UUID goalId, UUID contributorId) {
        boolean participates = goalMemberRepository.findById(new GoalMemberId(goalId, contributorId))
                .map(GoalMember::isContributes)
                .orElse(false);
        if (!participates) {
            throw new InvalidContributionException(
                    "Quem contribui precisa ser membro do objetivo e participar financeiramente.");
        }
    }

    static void requireActive(GoalAccess access, String conflictMessage) {
        if (access.goal().getStatus() != GoalStatus.ACTIVE) {
            throw new GoalStateConflictException(conflictMessage);
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    List<ContributionResponse> toResponses(Collection<Contribution> contributions,
            Map<UUID, ContributionReversal> reversals) {
        Set<UUID> people = new HashSet<>();
        contributions.forEach(c -> {
            people.add(c.getContributorId());
            people.add(c.getRegisteredById());
        });
        reversals.values().forEach(r -> people.add(r.getReversedById()));
        Map<UUID, String> names = userRepository.findAllById(people).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));

        return contributions.stream().map(c -> {
            ContributionReversal reversal = reversals.get(c.getId());
            return new ContributionResponse(c.getId(), c.getGoalId(),
                    person(c.getContributorId(), names), person(c.getRegisteredById(), names),
                    c.getAmount(), c.getContributedOn(), c.getNote(), c.getCreatedAt(), reversal != null,
                    reversal == null ? null : new ContributionResponse.Reversal(
                            person(reversal.getReversedById(), names), reversal.getReason(), reversal.getReversedAt()));
        }).toList();
    }

    private static ContributionResponse.Person person(UUID id, Map<UUID, String> names) {
        return new ContributionResponse.Person(id, names.get(id));
    }
}
