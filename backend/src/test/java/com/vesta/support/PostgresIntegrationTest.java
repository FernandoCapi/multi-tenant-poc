package com.vesta.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Shared PostgreSQL container, created with the same role script used by Docker Compose.
 * The application connects as app_user and Flyway as app_owner; the container superuser
 * is never handed to the application (which is why @ServiceConnection is not used).
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    protected static final String APP_USER = "app_user";
    protected static final String APP_USER_PASSWORD = "app-user-test";
    protected static final String APP_OWNER = "app_owner";
    protected static final String APP_OWNER_PASSWORD = "app-owner-test";
    protected static final String JWT_SECRET = "vesta-test-secret-with-at-least-32-bytes";

    private static final String SET_TENANT_SQL = "select set_config('app.tenant_id', ?, true)";

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18.6"))
                    .withDatabaseName("vesta")
                    .withEnv("APP_OWNER_PASSWORD", APP_OWNER_PASSWORD)
                    .withEnv("APP_USER_PASSWORD", APP_USER_PASSWORD)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath("../infra/postgres/init/01-create-roles.sh", 0755),
                            "/docker-entrypoint-initdb.d/01-create-roles.sh");

    static {
        // Singleton container shared by all test classes; Testcontainers removes it at JVM exit
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", () -> APP_USER_PASSWORD);
        registry.add("spring.flyway.user", () -> APP_OWNER);
        registry.add("spring.flyway.password", () -> APP_OWNER_PASSWORD);
        // Tests must not depend on the developer's .env
        registry.add("vesta.jwt.secret", () -> JWT_SECRET);
    }

    protected static Connection connectAsAppUser() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), APP_USER, APP_USER_PASSWORD);
    }

    protected static Connection connectAsOwner() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), APP_OWNER, APP_OWNER_PASSWORD);
    }

    /** Binds the tenant to the current transaction of the connection (auto-commit must be off). */
    protected static void setTenant(Connection connection, String tenant) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, tenant);
            statement.execute();
        }
    }

    protected static <T> T inTenantTransaction(Connection connection, Object tenant, SqlWork<T> work)
            throws SQLException {
        connection.setAutoCommit(false);
        try {
            setTenant(connection, tenant == null ? "" : tenant.toString());
            T result = work.run(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        }
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
