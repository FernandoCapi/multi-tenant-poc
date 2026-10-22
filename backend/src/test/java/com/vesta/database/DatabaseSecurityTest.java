package com.vesta.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vesta.support.PostgresIntegrationTest;

/**
 * Catalog checks for the guarantees RLS depends on (ADR-0001). The table check runs over
 * every table, so a table added later without forced RLS breaks the build.
 */
class DatabaseSecurityTest extends PostgresIntegrationTest {

    private static final String INSUFFICIENT_PRIVILEGE = "42501";

    @Test
    void applicationRoleIsNeitherSuperuserNorAllowedToBypassRls() throws SQLException {
        try (Connection connection = connectAsAppUser();
             Statement statement = connection.createStatement();
             ResultSet role = statement.executeQuery(
                     "select rolsuper, rolbypassrls from pg_roles where rolname = current_user")) {
            assertThat(role.next()).isTrue();
            assertThat(role.getBoolean("rolsuper")).isFalse();
            assertThat(role.getBoolean("rolbypassrls")).isFalse();
        }
    }

    @Test
    void applicationRoleOwnsNoTable() throws SQLException {
        try (Connection connection = connectAsAppUser();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "select count(*) from pg_tables where tableowner = current_user")) {
            rows.next();
            assertThat(rows.getLong(1)).isZero();
        }
    }

    @Test
    void everyApplicationTableHasForcedRowLevelSecurityAndAPolicy() throws SQLException {
        List<String> unprotected = new ArrayList<>();
        int tables = 0;
        try (Connection connection = connectAsOwner();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select c.relname,
                            c.relrowsecurity,
                            c.relforcerowsecurity,
                            (select count(*) from pg_policy p where p.polrelid = c.oid) as policies
                       from pg_class c
                       join pg_namespace n on n.oid = c.relnamespace
                      where n.nspname = 'public'
                        and c.relkind in ('r', 'p')
                        and c.relname <> 'flyway_schema_history'
                     """)) {
            while (rows.next()) {
                tables++;
                boolean protectedTable = rows.getBoolean("relrowsecurity")
                        && rows.getBoolean("relforcerowsecurity")
                        && rows.getLong("policies") > 0;
                if (!protectedTable) {
                    unprotected.add(rows.getString("relname"));
                }
            }
        }

        assertThat(tables).isGreaterThanOrEqualTo(6);
        assertThat(unprotected).as("tables without forced RLS and policy").isEmpty();
    }

    @Test
    void applicationRoleCannotDisableRowLevelSecurity() throws SQLException {
        try (Connection connection = connectAsAppUser();
             Statement statement = connection.createStatement()) {
            assertThatThrownBy(() -> statement.execute("alter table goals disable row level security"))
                    .isInstanceOfSatisfying(SQLException.class,
                            ex -> assertThat(ex.getSQLState()).isEqualTo(INSUFFICIENT_PRIVILEGE));
        }
    }
}
