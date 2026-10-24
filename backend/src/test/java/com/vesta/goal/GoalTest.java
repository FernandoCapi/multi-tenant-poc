package com.vesta.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Status rules of a goal (D-08, D-14, D-16). */
class GoalTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @Test
    void newGoalIsActive() {
        assertThat(newGoal().getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void completeAndReopen() {
        Goal goal = newGoal();

        goal.complete(NOW);
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(goal.getCompletedAt()).isEqualTo(NOW);

        goal.reopen();
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(goal.getCompletedAt()).isNull();
    }

    @Test
    void archiveFromActiveOrCompletedAndUnarchiveToActive() {
        Goal active = newGoal();
        active.archive(NOW);
        assertThat(active.getStatus()).isEqualTo(GoalStatus.ARCHIVED);
        assertThat(active.getArchivedAt()).isEqualTo(NOW);

        Goal completed = newGoal();
        completed.complete(NOW);
        completed.archive(NOW);
        completed.unarchive();
        assertThat(completed.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(completed.getArchivedAt()).isNull();
        assertThat(completed.getCompletedAt()).isNull();
    }

    @Test
    void invalidTransitionsConflict() {
        Goal goal = newGoal();
        assertThatThrownBy(goal::reopen).isInstanceOf(GoalStateConflictException.class);
        assertThatThrownBy(goal::unarchive).isInstanceOf(GoalStateConflictException.class);

        goal.complete(NOW);
        assertThatThrownBy(() -> goal.complete(NOW)).isInstanceOf(GoalStateConflictException.class);

        goal.archive(NOW);
        assertThatThrownBy(() -> goal.archive(NOW)).isInstanceOf(GoalStateConflictException.class);
        assertThatThrownBy(goal::reopen).isInstanceOf(GoalStateConflictException.class);
    }

    @Test
    void detailsChangeOnlyWhileActiveAndNullKeepsTheCurrentValue() {
        Goal goal = newGoal();
        goal.updateDetails("Viagem para a praia", null, new BigDecimal("1500.00"), null);
        assertThat(goal.getTitle()).isEqualTo("Viagem para a praia");
        assertThat(goal.getDescription()).isEqualTo("Descrição");
        assertThat(goal.getTargetAmount()).isEqualByComparingTo("1500.00");

        goal.complete(NOW);
        assertThatThrownBy(() -> goal.updateDetails("Outro", null, null, null))
                .isInstanceOf(GoalStateConflictException.class);
    }

    private static Goal newGoal() {
        return new Goal("Viagem", "Descrição", new BigDecimal("1000.00"), LocalDate.of(2027, 7, 1),
                UUID.randomUUID(), NOW);
    }
}
