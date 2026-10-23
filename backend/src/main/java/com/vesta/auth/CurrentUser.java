package com.vesta.auth;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** The authenticated user is always the subject of the validated token, never a request parameter. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            // The decoder already rejected tokens whose subject is not a UUID
            return UUID.fromString(token.getToken().getSubject());
        }
        throw new IllegalStateException("No authenticated user bound to the current thread");
    }
}
