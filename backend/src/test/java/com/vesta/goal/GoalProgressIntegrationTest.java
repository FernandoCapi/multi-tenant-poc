package com.vesta.goal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/**
 * Progress from real contributions (CA-02.3, CA-04.13, CA-04.14). The shared goal has 100.00
 * (owner) + 50.00 (member) and a reversed 30.00 that must not count.
 */
class GoalProgressIntegrationTest extends ApiIntegrationTest {

    private FamilyScenario family;

    @BeforeEach
    void createFamily() throws Exception {
        family = FamilyScenario.create("Progresso");
    }

    @Test
    void detailShowsProgressWithoutReversedContributionsAndTotalsPerMember() throws Exception {
        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, bearer(family, family.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.targetAmount").value(1000.00))
                .andExpect(jsonPath("$.progress.accumulatedAmount").value(150.00))
                .andExpect(jsonPath("$.progress.remainingAmount").value(850.00))
                .andExpect(jsonPath("$.progress.surplusAmount").value(0.00))
                .andExpect(jsonPath("$.progress.progressPercentage").value(15.00))
                .andExpect(jsonPath("$.progress.targetReached").value(false))
                .andExpect(jsonPath("$.contributionsByMember[0].userId").value(family.owner().toString()))
                .andExpect(jsonPath("$.contributionsByMember[0].amount").value(100.00))
                .andExpect(jsonPath("$.contributionsByMember[1].userId").value(family.member().toString()))
                .andExpect(jsonPath("$.contributionsByMember[1].amount").value(50.00));
    }

    @Test
    void listShowsTheSameProgressForVisibleGoalsOnly() throws Exception {
        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andExpect(jsonPath("$[?(@.id == '%s')].progress.accumulatedAmount".formatted(family.sharedGoal()))
                        .value(150.00))
                .andExpect(jsonPath("$[?(@.id == '%s')].progress.accumulatedAmount".formatted(family.privateGoal()))
                        .value(0.00));
    }

    @Test
    void reachingTheTargetDoesNotCompleteTheGoal() throws Exception {
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetAmount\": 120.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.progress.targetReached").value(true))
                .andExpect(jsonPath("$.progress.surplusAmount").value(30.00))
                .andExpect(jsonPath("$.progress.remainingAmount").value(0.00));
    }

    @Test
    void contributionsOfSomeoneWhoLeftStillCount() throws Exception {
        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.member())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.member())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andExpect(jsonPath("$.progress.accumulatedAmount").value(150.00))
                .andExpect(jsonPath("$.contributionsByMember[?(@.userId == '%s')].currentMember".formatted(family.member()))
                        .value(false));
    }
}
