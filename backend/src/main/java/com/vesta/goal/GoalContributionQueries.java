package com.vesta.goal;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Read-only questions about a goal's contributions, under row-level security of the transaction. */
@Component
public class GoalContributionQueries {

    private final JdbcTemplate jdbcTemplate;

    public GoalContributionQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Counts reversed contributions too: a goal that ever had one cannot be deleted (INV-11). */
    public boolean hasContributions(UUID goalId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "select exists (select 1 from contributions where goal_id = ?)", Boolean.class, goalId);
        return Boolean.TRUE.equals(exists);
    }
}
