package com.vesta.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vesta.support.FamilyFixture;
import com.vesta.support.PostgresIntegrationTest;

/**
 * Login lookup before any tenant is known (ADR-0001): one SECURITY DEFINER function owned by
 * app_auth, instead of a table without row-level security.
 */
class LoginCredentialsDatabaseTest extends PostgresIntegrationTest {

    private static final String INSUFFICIENT_PRIVILEGE = "42501";
    private static final String LOOKUP_SQL = "select user_id, family_id, password_hash from find_login_credentials(?)";

    private FamilyFixture family;

    @BeforeEach
    void createFamily() throws SQLException {
        family = FamilyFixture.create("Login");
    }

    @Test
    void findsTheUserByEmailWithoutTenantIgnoringCase() throws SQLException {
        try (Connection connection = connectAsAppUser();
             PreparedStatement statement = connection.prepareStatement(LOOKUP_SQL)) {
            statement.setString(1, family.email().toUpperCase());
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject("user_id", UUID.class)).isEqualTo(family.userId());
                assertThat(rows.getObject("family_id", UUID.class)).isEqualTo(family.familyId());
                assertThat(rows.getString("password_hash")).startsWith("{bcrypt}");
                assertThat(rows.next()).isFalse();
            }
        }
    }

    @Test
    void returnsNothingForAnUnknownEmail() throws SQLException {
        try (Connection connection = connectAsAppUser();
             PreparedStatement statement = connection.prepareStatement(LOOKUP_SQL)) {
            statement.setString(1, "ninguem@vesta.test");
            try (ResultSet rows = statement.executeQuery()) {
                assertThat(rows.next()).isFalse();
            }
        }
    }

    @Test
    void applicationRoleCannotReadPasswordHashesEvenInsideItsTenant() throws SQLException {
        try (Connection connection = connectAsAppUser()) {
            assertThatThrownBy(() -> inTenantTransaction(connection, family.familyId(), c -> {
                try (Statement statement = c.createStatement()) {
                    statement.executeQuery("select password_hash from users");
                }
                return null;
            })).isInstanceOfSatisfying(SQLException.class,
                    ex -> assertThat(ex.getSQLState()).isEqualTo(INSUFFICIENT_PRIVILEGE));
        }
    }

    @Test
    void lookupFunctionIsSecurityDefinerOwnedByANologinBypassRole() throws SQLException {
        try (Connection connection = connectAsOwner();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select p.prosecdef,
                            array_to_string(p.proconfig, ',') as config,
                            r.rolname,
                            r.rolcanlogin,
                            r.rolbypassrls
                       from pg_proc p
                       join pg_roles r on r.oid = p.proowner
                      where p.proname = 'find_login_credentials'
                     """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getBoolean("prosecdef")).isTrue();
            assertThat(rows.getString("config")).contains("search_path=public, pg_temp");
            assertThat(rows.getString("rolname")).isEqualTo("app_auth");
            assertThat(rows.getBoolean("rolcanlogin")).isFalse();
            assertThat(rows.getBoolean("rolbypassrls")).isTrue();
        }
    }

    @Test
    void onlyTheApplicationRoleMayExecuteTheLookup() throws SQLException {
        try (Connection connection = connectAsOwner();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select has_function_privilege('app_user', 'find_login_credentials(text)', 'EXECUTE') as app_user,
                            has_function_privilege('app_owner', 'find_login_credentials(text)', 'EXECUTE') as app_owner
                     """)) {
            rows.next();
            assertThat(rows.getBoolean("app_user")).isTrue();
            assertThat(rows.getBoolean("app_owner")).isFalse();
        }
    }

    @Test
    void schemaOwnerDoesNotInheritTheBypassOfTheLookupRole() throws SQLException {
        try (Connection connection = connectAsOwner()) {
            long visible = inTenantTransaction(connection, null, c -> {
                try (Statement statement = c.createStatement();
                     ResultSet rows = statement.executeQuery("select count(*) from users")) {
                    rows.next();
                    return rows.getLong(1);
                }
            });

            assertThat(visible).isZero();
        }
    }
}
