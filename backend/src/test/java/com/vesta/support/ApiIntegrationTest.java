package com.vesta.support;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.vesta.auth.TokenService;

/** HTTP-level tests authenticated with tokens issued by the real {@link TokenService}. */
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    protected String bearer(UUID userId, UUID familyId) {
        return "Bearer " + tokenService.issue(userId, familyId).accessToken();
    }

    protected String bearer(FamilyScenario family, UUID userId) {
        return bearer(userId, family.familyId());
    }
}
