package com.vesta.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.vesta.auth.TokenService;

class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void clearThreadState() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void bindsTheFamilyOfTheTokenOnlyWhileTheRequestRuns() throws Exception {
        UUID familyId = UUID.randomUUID();
        authenticateWithFamily(familyId);
        AtomicReference<Optional<UUID>> seenByRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> seenByRequest.set(TenantContext.get()));

        assertThat(seenByRequest.get()).contains(familyId);
        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void clearsTheTenantEvenWhenTheRequestFails() {
        authenticateWithFamily(UUID.randomUUID());

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> {
                    throw new ServletException("boom");
                })).isInstanceOf(ServletException.class);

        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void discardsATenantLeftOnThePooledThreadWhenTheRequestIsAnonymous() throws Exception {
        TenantContext.set(UUID.randomUUID());
        AtomicReference<Optional<UUID>> seenByRequest = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> seenByRequest.set(TenantContext.get()));

        assertThat(seenByRequest.get()).isEmpty();
    }

    private static void authenticateWithFamily(UUID familyId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(UUID.randomUUID().toString())
                .claim(TokenService.FAMILY_ID_CLAIM, familyId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
