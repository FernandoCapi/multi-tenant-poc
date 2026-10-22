package com.vesta.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the access tokens issued by the backend (ADR-0002).
 * The HS256 secret must have at least 256 bits; the application refuses to start otherwise.
 */
@ConfigurationProperties("vesta.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("60m") Duration ttl,
        @DefaultValue("vesta") String issuer) {

    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "vesta.jwt.secret (JWT_SECRET) must have at least " + MIN_SECRET_BYTES + " bytes");
        }
    }
}
