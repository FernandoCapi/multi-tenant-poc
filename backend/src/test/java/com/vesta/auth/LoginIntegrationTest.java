package com.vesta.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.vesta.support.FamilyFixture;
import com.vesta.support.PostgresIntegrationTest;

/** Login (RF-05, CA-05.2; demonstration scenario N-9). */
@AutoConfigureMockMvc
class LoginIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    private FamilyFixture family;

    @BeforeEach
    void createFamily() throws Exception {
        family = FamilyFixture.create("Login");
    }

    @Test
    void validCredentialsReturnATokenForTheUserAndTheirFamily() throws Exception {
        MvcResult result = mockMvc.perform(login(family.email(), FamilyFixture.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(family.userId().toString());
        assertThat(jwt.getClaimAsString(TokenService.FAMILY_ID_CLAIM)).isEqualTo(family.familyId().toString());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        mockMvc.perform(login(family.email().toUpperCase(), FamilyFixture.PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        MvcResult wrongPassword = mockMvc.perform(login(family.email(), "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."))
                .andReturn();
        MvcResult unknownEmail = mockMvc.perform(login(UUID.randomUUID() + "@vesta.test", FamilyFixture.PASSWORD))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(unknownEmail.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void blankCredentialsAreRejectedAsInvalidData() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}""".formatted(email, password));
    }
}
