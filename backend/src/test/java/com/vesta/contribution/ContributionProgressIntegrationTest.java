package com.vesta.contribution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/**
 * Registering and reversing change the goal's progress (CA-04.1, CA-04.9, CA-04.14). The shared
 * goal starts with 150.00 accumulated: 100.00 (owner) + 50.00 (member).
 */
class ContributionProgressIntegrationTest extends ApiIntegrationTest {

    private FamilyScenario family;

    @BeforeEach
    void createFamily() throws Exception {
        family = FamilyScenario.create("Progresso");
    }

    @Test
    void registeringAddsToTheAccumulatedAndToTheContributorTotal() throws Exception {
        mockMvc.perform(post("/api/goals/{id}/contributions", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.member()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 25.50}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andExpect(jsonPath("$.progress.accumulatedAmount").value(175.50))
                .andExpect(jsonPath("$.progress.remainingAmount").value(824.50))
                .andExpect(jsonPath("$.contributionsByMember[?(@.userId == '%s')].amount".formatted(family.member()))
                        .value(75.50));
    }
}
