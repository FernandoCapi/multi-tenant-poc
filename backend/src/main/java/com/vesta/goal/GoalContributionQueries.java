package com.vesta.goal;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-only questions about contributions, under the row-level security of the transaction.
 * Callers pass only goals the user can see, so totals never include inaccessible goals (CA-03.8).
 */
@Component
public class GoalContributionQueries {

    private static final String NOT_REVERSED =
            "not exists (select 1 from contribution_reversals r where r.contribution_id = c.id)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GoalContributionQueries(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Counts reversed contributions too: a goal that ever had one cannot be deleted (INV-11). */
    public boolean hasContributions(UUID goalId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from contributions where goal_id = :goalId)",
                Map.of("goalId", goalId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /** Sum of the non-reversed contributions of each goal; goals without any are absent. */
    public Map<UUID, BigDecimal> accumulatedByGoal(Collection<UUID> goalIds) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        if (goalIds.isEmpty()) {
            return totals;
        }
        jdbcTemplate.query("""
                        select c.goal_id, sum(c.amount) as total
                          from contributions c
                         where c.goal_id in (:goalIds) and %s
                         group by c.goal_id
                        """.formatted(NOT_REVERSED),
                Map.of("goalIds", goalIds),
                row -> {
                    totals.put(row.getObject("goal_id", UUID.class), row.getBigDecimal("total"));
                });
        return totals;
    }

    /** Non-reversed total per contributor, including people who have since left the goal (D-07). */
    public Map<UUID, BigDecimal> accumulatedByContributor(UUID goalId) {
        Map<UUID, BigDecimal> totals = new HashMap<>();
        jdbcTemplate.query("""
                        select c.contributor_id, sum(c.amount) as total
                          from contributions c
                         where c.goal_id = :goalId and %s
                         group by c.contributor_id
                        """.formatted(NOT_REVERSED),
                Map.of("goalId", goalId),
                row -> {
                    totals.put(row.getObject("contributor_id", UUID.class), row.getBigDecimal("total"));
                });
        return totals;
    }
}
