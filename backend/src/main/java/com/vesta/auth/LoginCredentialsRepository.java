package com.vesta.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Login lookup through the find_login_credentials SECURITY DEFINER function (ADR-0001): it is the
 * only way to find a user before a tenant is known, and the only way to read password hashes.
 */
@Repository
public class LoginCredentialsRepository {

    private static final String LOOKUP_SQL =
            "select user_id, family_id, password_hash from find_login_credentials(?)";

    private final JdbcTemplate jdbcTemplate;

    public LoginCredentialsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<LoginCredentials> findByEmail(String email) {
        return jdbcTemplate.query(LOOKUP_SQL, (row, rowNumber) -> new LoginCredentials(
                        row.getObject("user_id", UUID.class),
                        row.getObject("family_id", UUID.class),
                        row.getString("password_hash")), email)
                .stream()
                .findFirst();
    }

    public record LoginCredentials(UUID userId, UUID familyId, String passwordHash) {
    }
}
