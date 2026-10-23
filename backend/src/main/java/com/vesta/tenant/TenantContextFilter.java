package com.vesta.tenant;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vesta.auth.TokenService;

/**
 * Binds the family of the validated access token to the request thread (DT-04, RS-04).
 * The tenant comes only from the signed token issued at login, never from request parameters,
 * headers or bodies, and is always cleared when the request ends.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantContext.clear();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            // The decoder already rejected tokens whose family_id is not a UUID
            TenantContext.set(UUID.fromString(token.getToken().getClaimAsString(TokenService.FAMILY_ID_CLAIM)));
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
