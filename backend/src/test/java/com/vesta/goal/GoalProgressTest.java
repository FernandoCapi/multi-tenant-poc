package com.vesta.goal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** Calculation rules of docs/requisitos.md §6 (D-12). */
class GoalProgressTest {

    @Test
    void belowTheTarget() {
        GoalProgress progress = GoalProgress.of(new BigDecimal("1000.00"), new BigDecimal("150.00"));

        assertThat(progress.accumulatedAmount()).isEqualByComparingTo("150.00");
        assertThat(progress.remainingAmount()).isEqualByComparingTo("850.00");
        assertThat(progress.surplusAmount()).isEqualByComparingTo("0.00");
        assertThat(progress.progressPercentage()).isEqualByComparingTo("15.00");
        assertThat(progress.targetReached()).isFalse();
    }

    @Test
    void exactlyOnTheTarget() {
        GoalProgress progress = GoalProgress.of(new BigDecimal("2500.00"), new BigDecimal("2500.00"));

        assertThat(progress.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(progress.surplusAmount()).isEqualByComparingTo("0.00");
        assertThat(progress.progressPercentage()).isEqualByComparingTo("100.00");
        assertThat(progress.targetReached()).isTrue();
    }

    @Test
    void surplusIsAcceptedAndShown() {
        GoalProgress progress = GoalProgress.of(new BigDecimal("100.00"), new BigDecimal("130.50"));

        assertThat(progress.remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(progress.surplusAmount()).isEqualByComparingTo("30.50");
        assertThat(progress.progressPercentage()).isEqualByComparingTo("130.50");
        assertThat(progress.targetReached()).isTrue();
    }

    @Test
    void nothingContributedYet() {
        GoalProgress progress = GoalProgress.of(new BigDecimal("3500.00"), BigDecimal.ZERO);

        assertThat(progress.accumulatedAmount()).isEqualByComparingTo("0.00");
        assertThat(progress.remainingAmount()).isEqualByComparingTo("3500.00");
        assertThat(progress.progressPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    void percentageIsRoundedToTwoPlaces() {
        assertThat(GoalProgress.of(new BigDecimal("300.00"), new BigDecimal("100.00")).progressPercentage())
                .isEqualByComparingTo("33.33");
    }
}
