package com.vesta.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import com.vesta.goal.Goal;
import com.vesta.goal.GoalRepository;
import com.vesta.support.FamilyFixture;
import com.vesta.support.PostgresIntegrationTest;

/**
 * Application-level isolation through Spring transactions (RS-05, RS-06, RS-07).
 * A single pooled connection makes every transaction reuse the same database session, which is
 * the worst case for tenant leakage.
 */
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=1")
class TenantIsolationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FamilyFixture familyA;
    private FamilyFixture familyB;

    @BeforeEach
    void createFamilies() throws SQLException {
        familyA = FamilyFixture.create("A");
        familyB = FamilyFixture.create("B");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void repositoryReturnsOnlyGoalsOfTheBoundFamily() {
        TenantContext.set(familyA.familyId());

        List<Goal> goals = transactionTemplate.execute(status -> goalRepository.findAll());

        assertThat(goals).extracting(Goal::getId).contains(familyA.goalId()).doesNotContain(familyB.goalId());
        assertThat(goals).extracting(Goal::getFamilyId).containsOnly(familyA.familyId());
    }

    @Test
    void nativeSqlWithoutWhereSeesOnlyTheBoundFamily() {
        TenantContext.set(familyA.familyId());

        // Plain SQL bypasses Hibernate's tenant filter; only row-level security restricts it
        List<UUID> families = transactionTemplate.execute(
                status -> jdbcTemplate.queryForList("select family_id from goals", UUID.class));

        assertThat(families).isNotEmpty().containsOnly(familyA.familyId());
    }

    @Test
    void tenantDoesNotLeakBetweenTransactionsOnThePooledConnection() {
        TenantContext.set(familyA.familyId());
        Visibility first = visibleFamilies();

        TenantContext.set(familyB.familyId());
        Visibility second = visibleFamilies();

        TenantContext.clear();
        Visibility third = visibleFamilies();

        assertThat(List.of(first.backendPid(), second.backendPid(), third.backendPid()))
                .as("same database session").containsOnly(first.backendPid());
        assertThat(first.families()).isNotEmpty().containsOnly(familyA.familyId());
        assertThat(second.families()).isNotEmpty().containsOnly(familyB.familyId());
        assertThat(third.families()).isEmpty();
    }

    @Test
    void savedGoalBelongsToTheBoundFamily() throws SQLException {
        TenantContext.set(familyA.familyId());

        Goal saved = transactionTemplate.execute(status -> goalRepository.saveAndFlush(
                new Goal("Reserva de emergência", new BigDecimal("5000.00"), familyA.userId())));

        assertThat(saved.getFamilyId()).isEqualTo(familyA.familyId());
        assertThat(storedFamilyOf(saved.getId())).isEqualTo(familyA.familyId());
    }

    @Test
    void withoutBoundTenantReadsAreEmptyAndWritesFail() {
        List<Goal> goals = transactionTemplate.execute(status -> goalRepository.findAll());

        assertThat(goals).isEmpty();
        assertThatThrownBy(() -> transactionTemplate.execute(status -> goalRepository.saveAndFlush(
                new Goal("Sem família", new BigDecimal("10.00"), familyA.userId()))))
                .isInstanceOf(DataAccessException.class);
    }

    private Visibility visibleFamilies() {
        return transactionTemplate.execute(status -> new Visibility(
                jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class),
                jdbcTemplate.queryForList("select family_id from goals", UUID.class)));
    }

    private UUID storedFamilyOf(UUID goalId) throws SQLException {
        try (Connection owner = connectAsOwner()) {
            return inTenantTransaction(owner, familyA.familyId(), connection -> {
                try (PreparedStatement statement =
                             connection.prepareStatement("select family_id from goals where id = ?")) {
                    statement.setObject(1, goalId);
                    try (ResultSet rows = statement.executeQuery()) {
                        rows.next();
                        return rows.getObject(1, UUID.class);
                    }
                }
            });
        }
    }

    private record Visibility(Integer backendPid, List<UUID> families) {
    }
}
