package com.vesta.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.vesta.auth.TokenService;
import com.vesta.support.FamilyFixture;
import com.vesta.support.PostgresIntegrationTest;

/**
 * The tenant of every request comes only from the signed token (RS-03, RS-04; scenarios N-2, N-8).
 */
@AutoConfigureMockMvc
class MeIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    private FamilyFixture familyA;
    private FamilyFixture familyB;

    @BeforeEach
    void createFamilies() throws Exception {
        familyA = FamilyFixture.create("A");
        familyB = FamilyFixture.create("B");
    }

    @Test
    void loginTokenGivesAccessToTheUserAndFamilyOfThatLogin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(familyB.email(), FamilyFixture.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(familyB.userId().toString()))
                .andExpect(jsonPath("$.email").value(familyB.email()))
                .andExpect(jsonPath("$.family.id").value(familyB.familyId().toString()))
                .andExpect(jsonPath("$.family.name").value("Família B"));
    }

    @Test
    void familySentByTheClientIsIgnored() throws Exception {
        mockMvc.perform(get("/api/me")
                        .param("family_id", familyB.familyId().toString())
                        .header("X-Family-Id", familyB.familyId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(validToken(familyA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.family.id").value(familyA.familyId().toString()));
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS);
        String token = encode(jwtEncoder, claims(familyA, issuedAt, issuedAt.plus(1, ChronoUnit.HOURS)));

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithTamperedFamilyIsUnauthorized() throws Exception {
        String[] parts = validToken(familyA).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace(familyA.familyId().toString(), familyB.familyId().toString());
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyIsUnauthorized() throws Exception {
        JwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "another-secret-with-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        String token = encode(foreignEncoder, claims(familyA, now, now.plus(1, ChronoUnit.HOURS)));

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutFamilyIsUnauthorized() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vesta")
                .subject(familyA.userId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .build();

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(encode(jwtEncoder, claims))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWhoseUserIsNotInItsFamilyIsUnauthorized() throws Exception {
        // Validly signed, but pairing the user of A with the family of B: the user is not visible
        String token = tokenService.issue(familyA.userId(), familyB.familyId()).accessToken();

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    private String validToken(FamilyFixture family) {
        return tokenService.issue(family.userId(), family.familyId()).accessToken();
    }

    private static JwtClaimsSet claims(FamilyFixture family, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .issuer("vesta")
                .subject(family.userId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TokenService.FAMILY_ID_CLAIM, family.familyId().toString())
                .build();
    }

    private static String encode(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
