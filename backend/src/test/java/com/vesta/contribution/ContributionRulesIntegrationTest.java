package com.vesta.contribution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/** Validation and state rules of contributions (CA-04.1..CA-04.7, D-12, D-18; scenario N-7). */
class ContributionRulesIntegrationTest extends ApiIntegrationTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private FamilyScenario family;
    private String owner;

    @BeforeEach
    void createFamily() throws Exception {
        family = FamilyScenario.create("Regras");
        owner = bearer(family, family.owner());
    }

    @Test
    void ownerRegistersOnBehalfOfAParticipant() throws Exception {
        register(owner, "{\"amount\": 40.00, \"contributorId\": \"%s\", \"note\": \"Mesada\"}".formatted(family.member()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributor.id").value(family.member().toString()))
                .andExpect(jsonPath("$.contributor.displayName").value("Membro Regras"))
                .andExpect(jsonPath("$.registeredBy.id").value(family.owner().toString()))
                .andExpect(jsonPath("$.note").value("Mesada"))
                .andExpect(jsonPath("$.reversed").value(false));
    }

    @Test
    void dateDefaultsToTodayInSaoPaulo() throws Exception {
        register(owner, "{\"amount\": 10.00}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributedOn").value(LocalDate.now(SAO_PAULO).toString()));
    }

    @Test
    void pastDatesAreAcceptedAndFutureDatesRejected() throws Exception {
        LocalDate today = LocalDate.now(SAO_PAULO);
        register(owner, "{\"amount\": 10.00, \"contributedOn\": \"%s\"}".formatted(today.minusDays(10)))
                .andExpect(status().isCreated());
        register(owner, "{\"amount\": 10.00, \"contributedOn\": \"%s\"}".formatted(today.plusDays(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A data da contribuição não pode ser futura."));
    }

    @Test
    void invalidAmountsAndNotesAreRejected() throws Exception {
        register(owner, "{\"amount\": 0}").andExpect(status().isUnprocessableContent());
        register(owner, "{\"amount\": -5.00}").andExpect(status().isUnprocessableContent());
        register(owner, "{\"amount\": 10.001}").andExpect(status().isUnprocessableContent());
        register(owner, "{}").andExpect(status().isUnprocessableContent());
        register(owner, "{\"amount\": 10.00, \"note\": \"%s\"}".formatted("x".repeat(501)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void contributorMustBeAParticipatingMemberOfTheGoal() throws Exception {
        String message = "Quem contribui precisa ser membro do objetivo e participar financeiramente.";
        // Member who does not contribute (D-05)
        register(owner, "{\"amount\": 10.00, \"contributorId\": \"%s\"}".formatted(family.viewer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(message));
        // Same family, not a member of the goal
        register(owner, "{\"amount\": 10.00, \"contributorId\": \"%s\"}".formatted(family.outsider()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(message));
        register(owner, "{\"amount\": 10.00, \"contributorId\": \"%s\"}".formatted(UUID.randomUUID()))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void onlyActiveGoalsReceiveContributions() throws Exception {
        // N-7
        mockMvc.perform(post("/api/goals/{id}/complete", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());
        register(owner, "{\"amount\": 10.00}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Só objetivos ativos recebem contribuições."));

        mockMvc.perform(post("/api/goals/{id}/archive", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());
        register(owner, "{\"amount\": 10.00}").andExpect(status().isConflict());
    }

    @Test
    void permissionIsCheckedBeforeTheBody() throws Exception {
        register(bearer(family, family.viewer()), "{\"amount\": -1}").andExpect(status().isForbidden());
        register(bearer(family, family.outsider()), "{\"amount\": -1}").andExpect(status().isNotFound());
    }

    private ResultActions register(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/goals/{id}/contributions", family.sharedGoal())
                .header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
