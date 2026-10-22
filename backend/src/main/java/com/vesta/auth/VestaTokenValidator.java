package com.vesta.auth;

import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Rejects tokens whose subject or family_id is not a UUID, so every accepted token carries a tenant. */
class VestaTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error MISSING_IDENTITY = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "Token without a valid subject or family_id", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (isUuid(token.getSubject()) && isUuid(token.getClaimAsString(TokenService.FAMILY_ID_CLAIM))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(MISSING_IDENTITY);
    }

    private static boolean isUuid(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
