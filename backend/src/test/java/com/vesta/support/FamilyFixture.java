package com.vesta.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A family with one row in every business table: a user, a goal with its owner membership,
 * one active contribution and one reversed contribution.
 * Inserted by the schema owner inside a transaction bound to the new family, because the
 * policies are forced for the owner too.
 */
public record FamilyFixture(
        UUID familyId,
        UUID userId,
        String goalTitle,
        UUID goalId,
        UUID contributionId,
        UUID reversedContributionId) {

    /** Password of every fixture user; the hash below was generated with Spring's delegating encoder. */
    public static final String PASSWORD = "senha-teste";
    private static final String PASSWORD_HASH =
            "{bcrypt}$2a$10$i5aGPYBayFksFaLp5mAxcOqfB5aUHpIMqrqXMb4TJ9bOnuh1r7Dgm";

    public static FamilyFixture create(String name) throws SQLException {
        FamilyFixture family = new FamilyFixture(
                UUID.randomUUID(), UUID.randomUUID(), "Viagem " + name,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        try (Connection owner = PostgresIntegrationTest.connectAsOwner()) {
            PostgresIntegrationTest.inTenantTransaction(owner, family.familyId(), connection -> {
                family.insert(connection, name);
                return null;
            });
        }
        return family;
    }

    private void insert(Connection connection, String name) throws SQLException {
        execute(connection, "insert into families (id, name) values (?, ?)", familyId, "Família " + name);
        execute(connection,
                "insert into users (id, family_id, email, display_name, password_hash) values (?, ?, ?, ?, ?)",
                userId, familyId, email(), "Responsável " + name, PASSWORD_HASH);
        execute(connection,
                "insert into goals (id, family_id, title, target_amount, created_by) values (?, ?, ?, 1000.00, ?)",
                goalId, familyId, goalTitle, userId);
        execute(connection,
                "insert into goal_members (family_id, goal_id, user_id, role, added_by) values (?, ?, ?, 'OWNER', ?)",
                familyId, goalId, userId, userId);
        insertContribution(connection, contributionId);
        insertContribution(connection, reversedContributionId);
        execute(connection,
                "insert into contribution_reversals (family_id, contribution_id, reversed_by_id, reason) "
                        + "values (?, ?, ?, 'Valor digitado errado')",
                familyId, reversedContributionId, userId);
    }

    public String email() {
        return userId + "@vesta.test";
    }

    private void insertContribution(Connection connection, UUID id) throws SQLException {
        execute(connection,
                "insert into contributions (id, family_id, goal_id, contributor_id, registered_by_id, amount, "
                        + "contributed_on) values (?, ?, ?, ?, ?, 100.00, current_date)",
                id, familyId, goalId, userId, userId);
    }

    private static void execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.executeUpdate();
        }
    }
}
