package com.vesta.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.vesta.support.PostgresIntegrationTest;

/** The "local" profile loads the demo families and their users can log in (D-11, DT-05). */
@ActiveProfiles("local")
@AutoConfigureMockMvc
class DemoDataIntegrationTest extends PostgresIntegrationTest {

    private static final String DEMO_PASSWORD = "vesta-demo";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "carlos@silva.demo, Carlos, Família Silva",
            "pedro@silva.demo, Pedro, Família Silva",
            "marcos@souza.demo, Marcos, Família Souza"})
    void demoUsersLogInToTheirOwnFamily(String email, String displayName, String familyName) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}""".formatted(email, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(displayName))
                .andExpect(jsonPath("$.family.name").value(familyName));
    }
}
