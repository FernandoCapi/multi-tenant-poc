package com.vesta.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vesta.support.FamilyFixture;
import com.vesta.support.PostgresIntegrationTest;

/**
 * Database-level isolation, exercised as app_user with plain SQL and no WHERE family_id
 * (RS-04, RS-05, RS-06; demonstration scenario N-3).
 */
class RowLevelSecurityTest extends PostgresIntegrationTest {

    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String INSUFFICIENT_PRIVILEGE = "42501";
    private static final String INVALID_TEXT_REPRESENTATION = "22P02";

    private FamilyFixture familyA;
    private FamilyFixture familyB;

    @BeforeEach
    void createFamilies() throws SQLException {
        familyA = FamilyFixture.create("A");
        familyB = FamilyFixture.create("B");
    }

    @ParameterizedTest
    @ValueSource(strings = {"users", "goals", "goal_members", "contributions", "contribution_reversals"})
    void selectWithoutWhereReturnsOnlyRowsOfTheBoundFamily(String table) throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            List<UUID> families = inTenantTransaction(connection, familyA.familyId(),
                    c -> uuids(c, "select family_id from " + table));

            assertThat(families).isNotEmpty().containsOnly(familyA.familyId());
        }
    }

    @Test
    void familiesTableShowsOnlyTheBoundFamily() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            List<UUID> ids = inTenantTransaction(connection, familyA.familyId(),
                    c -> uuids(c, "select id from families"));

            assertThat(ids).containsExactly(familyA.familyId());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"families", "users", "goals", "goal_members", "contributions", "contribution_reversals"})
    void withoutBoundTenantNoRowIsVisible(String table) throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            long count = inTenantTransaction(connection, null, c -> count(c, "select count(*) from " + table));

            assertThat(count).isZero();
        }
    }

    @Test
    void tenantBindingEndsWithTheTransactionOnTheSameConnection() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            long visibleWithTenant = inTenantTransaction(connection, familyA.familyId(),
                    c -> count(c, "select count(*) from goals"));

            // Next transaction on the same physical connection, without binding a tenant:
            // the setting now reads as '' and must still match nothing
            connection.setAutoCommit(false);
            String setting = text(connection, "select current_setting('app.tenant_id', true)");
            long visibleWithoutTenant = count(connection, "select count(*) from goals");
            connection.commit();

            assertThat(visibleWithTenant).isPositive();
            assertThat(setting).isEmpty();
            assertThat(visibleWithoutTenant).isZero();
        }
    }

    @Test
    void insertForAnotherFamilyIsRejectedByThePolicy() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, familyA.familyId(), c -> update(c,
                    "insert into goals (family_id, title, target_amount, created_by) values (?, 'Forjado', 10.00, ?)",
                    familyB.familyId(), familyB.userId())))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("row-level security");
        }
    }

    @Test
    void updateAndDeleteCannotReachRowsOfAnotherFamily() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            int updated = inTenantTransaction(connection, familyA.familyId(),
                    c -> update(c, "update goals set title = 'Alterado' where id = ?", familyB.goalId()));
            int deleted = inTenantTransaction(connection, familyA.familyId(),
                    c -> update(c, "delete from goal_members where goal_id = ?", familyB.goalId()));

            assertThat(updated).isZero();
            assertThat(deleted).isZero();
        }
        try (Connection connection = connectAsAppUser()) {
            String title = inTenantTransaction(connection, familyB.familyId(),
                    c -> text(c, "select title from goals where id = '" + familyB.goalId() + "'"));
            long members = inTenantTransaction(connection, familyB.familyId(),
                    c -> count(c, "select count(*) from goal_members where goal_id = '" + familyB.goalId() + "'"));

            assertThat(title).isEqualTo(familyB.goalTitle());
            assertThat(members).isEqualTo(1);
        }
    }

    @Test
    void referenceToAGoalOfAnotherFamilyIsRejectedByTheCompositeForeignKey() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, familyA.familyId(), c -> update(c,
                    "insert into goal_members (family_id, goal_id, user_id, role, added_by) "
                            + "values (?, ?, ?, 'MEMBER', ?)",
                    familyA.familyId(), familyB.goalId(), familyA.userId(), familyA.userId())))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(FOREIGN_KEY_VIOLATION));
        }
    }

    @Test
    void invalidTenantValueFailsInsteadOfReturningRows() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, "not-a-uuid",
                    c -> count(c, "select count(*) from goals")))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(INVALID_TEXT_REPRESENTATION));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "update contributions set amount = 1",
            "delete from contributions",
            "update contribution_reversals set reason = 'Outro motivo'",
            "delete from contribution_reversals"})
    void contributionsAndReversalsAreImmutableForTheApplication(String statement) throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, familyA.familyId(), c -> update(c, statement)))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(INSUFFICIENT_PRIVILEGE));
        }
    }

    @Test
    void aContributionCanBeReversedOnlyOnce() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, familyA.familyId(), c -> update(c,
                    "insert into contribution_reversals (family_id, contribution_id, reversed_by_id, reason) "
                            + "values (?, ?, ?, 'Segundo estorno')",
                    familyA.familyId(), familyA.reversedContributionId(), familyA.userId())))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(UNIQUE_VIOLATION));
        }
    }

    private static List<UUID> uuids(Connection connection, String sql) throws SQLException {
        List<UUID> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                values.add(rows.getObject(1, UUID.class));
            }
        }
        return values;
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static String text(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getString(1);
        }
    }

    private static int update(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            return statement.executeUpdate();
        }
    }
}
