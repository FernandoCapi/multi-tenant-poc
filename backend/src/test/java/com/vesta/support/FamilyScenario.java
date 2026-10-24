package com.vesta.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * A family with every profile of the permission matrix (docs/requisitos.md §7):
 * <ul>
 *   <li>{@code owner}: OWNER of both goals;</li>
 *   <li>{@code member}: MEMBER of the shared goal, contributes;</li>
 *   <li>{@code viewer}: MEMBER of the shared goal, does not contribute;</li>
 *   <li>{@code outsider}: same family, no link to any goal (the "child × rent" case).</li>
 * </ul>
 * The shared goal (target 1000.00) has contributions of 100.00 (owner) and 50.00 (member), plus a
 * reversed 30.00 (member). The private goal belongs to the owner alone and has no contributions.
 */
public record FamilyScenario(
        UUID familyId,
        UUID owner,
        UUID member,
        UUID viewer,
        UUID outsider,
        UUID sharedGoal,
        UUID privateGoal,
        UUID ownerContribution,
        UUID memberContribution,
        UUID reversedContribution) {

    private static final String PASSWORD_HASH =
            "{bcrypt}$2a$10$i5aGPYBayFksFaLp5mAxcOqfB5aUHpIMqrqXMb4TJ9bOnuh1r7Dgm";

    public static FamilyScenario create(String name) throws SQLException {
        FamilyScenario scenario = new FamilyScenario(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        try (Connection owner = PostgresIntegrationTest.connectAsOwner()) {
            PostgresIntegrationTest.inTenantTransaction(owner, scenario.familyId(), connection -> {
                scenario.insert(connection, name);
                return null;
            });
        }
        return scenario;
    }

    private void insert(Connection connection, String name) throws SQLException {
        execute(connection, "insert into families (id, name) values (?, ?)", familyId, "Família " + name);
        insertUser(connection, owner, "Dono " + name);
        insertUser(connection, member, "Membro " + name);
        insertUser(connection, viewer, "Leitor " + name);
        insertUser(connection, outsider, "Sem vínculo " + name);

        insertGoal(connection, sharedGoal, "Viagem " + name);
        insertMember(connection, sharedGoal, owner, "OWNER", true);
        insertMember(connection, sharedGoal, member, "MEMBER", true);
        insertMember(connection, sharedGoal, viewer, "MEMBER", false);

        insertGoal(connection, privateGoal, "Aluguel " + name);
        insertMember(connection, privateGoal, owner, "OWNER", true);

        insertContribution(connection, ownerContribution, owner, "100.00");
        insertContribution(connection, memberContribution, member, "50.00");
        insertContribution(connection, reversedContribution, member, "30.00");
        execute(connection,
                "insert into contribution_reversals (family_id, contribution_id, reversed_by_id, reason) "
                        + "values (?, ?, ?, 'Valor errado')",
                familyId, reversedContribution, member);
    }

    private void insertUser(Connection connection, UUID id, String displayName) throws SQLException {
        execute(connection,
                "insert into users (id, family_id, email, display_name, password_hash) values (?, ?, ?, ?, ?)",
                id, familyId, id + "@vesta.test", displayName, PASSWORD_HASH);
    }

    private void insertGoal(Connection connection, UUID id, String title) throws SQLException {
        execute(connection,
                "insert into goals (id, family_id, title, target_amount, created_by) values (?, ?, ?, 1000.00, ?)",
                id, familyId, title, owner);
    }

    private void insertMember(Connection connection, UUID goalId, UUID userId, String role, boolean contributes)
            throws SQLException {
        execute(connection,
                "insert into goal_members (family_id, goal_id, user_id, role, contributes, added_by) "
                        + "values (?, ?, ?, ?, ?, ?)",
                familyId, goalId, userId, role, contributes, owner);
    }

    private void insertContribution(Connection connection, UUID id, UUID contributor, String amount)
            throws SQLException {
        execute(connection,
                "insert into contributions (id, family_id, goal_id, contributor_id, registered_by_id, amount, "
                        + "contributed_on) values (?, ?, ?, ?, ?, ?::numeric, current_date)",
                id, familyId, sharedGoal, contributor, contributor, amount);
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
