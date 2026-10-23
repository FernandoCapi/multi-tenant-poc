package com.vesta.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Financial progress of a goal (docs/requisitos.md §6), in exact decimals with two places.
 * The accumulated amount counts only contributions that were not reversed. Reaching the target is
 * an indicator only: it never changes the goal's status (D-08).
 */
public record GoalProgress(
        BigDecimal targetAmount,
        BigDecimal accumulatedAmount,
        BigDecimal remainingAmount,
        BigDecimal surplusAmount,
        BigDecimal progressPercentage,
        boolean targetReached) {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static GoalProgress of(BigDecimal targetAmount, BigDecimal accumulatedAmount) {
        BigDecimal target = targetAmount.setScale(SCALE, RoundingMode.UNNECESSARY);
        BigDecimal accumulated = accumulatedAmount.setScale(SCALE, RoundingMode.UNNECESSARY);
        BigDecimal difference = target.subtract(accumulated);
        return new GoalProgress(
                target,
                accumulated,
                difference.max(BigDecimal.ZERO).setScale(SCALE),
                difference.negate().max(BigDecimal.ZERO).setScale(SCALE),
                accumulated.multiply(HUNDRED).divide(target, SCALE, RoundingMode.HALF_UP),
                accumulated.compareTo(target) >= 0);
    }
}
