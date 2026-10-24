package com.vesta.contribution;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/** Paginated listing of a goal's contributions (CA-04.8, CA-04.13, D-19). */
class ContributionListingIntegrationTest extends ApiIntegrationTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private FamilyScenario family;
    private FamilyScenario otherFamily;

    @BeforeEach
    void createFamilies() throws Exception {
        family = FamilyScenario.create("A");
        otherFamily = FamilyScenario.create("B");
    }

    @Test
    void everyMemberSeesEveryContributionWithReversalsFlagged() throws Exception {
        // The viewer does not contribute, but sees everything (CA-03.7)
        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].reversed".formatted(family.reversedContribution()))
                        .value(true))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].reversal.reason".formatted(family.reversedContribution()))
                        .value("Valor errado"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].reversed".formatted(family.ownerContribution()))
                        .value(false));
    }

    @Test
    void sumOfTheNonReversedContributionsEqualsTheAccumulated() throws Exception {
        String list = mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andReturn().getResponse().getContentAsString();
        List<Number> counted = JsonPath.read(list, "$.content[?(@.reversed == false)].amount");
        BigDecimal sum = counted.stream().map(n -> new BigDecimal(n.toString())).reduce(BigDecimal.ZERO, BigDecimal::add);

        String detail = mockMvc.perform(get("/api/goals/{id}", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andReturn().getResponse().getContentAsString();
        Number accumulated = JsonPath.read(detail, "$.progress.accumulatedAmount");

        Assertions.assertThat(sum).isEqualByComparingTo(accumulated.toString());
    }

    @Test
    void mostRecentFirstAndPaginated() throws Exception {
        LocalDate today = LocalDate.now(SAO_PAULO);
        registerOnPrivateGoal("10.00", today.minusDays(5));
        registerOnPrivateGoal("20.00", today.minusDays(1));
        registerOnPrivateGoal("30.00", today.minusDays(3));
        String owner = bearer(family, family.owner());

        mockMvc.perform(get("/api/goals/{id}/contributions", family.privateGoal()).param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(20.00))
                .andExpect(jsonPath("$.content[1].amount").value(30.00));

        mockMvc.perform(get("/api/goals/{id}/contributions", family.privateGoal()).param("size", "2").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(10.00));
    }

    @Test
    void pageSizeIsCappedAndInvalidPagesRejected() throws Exception {
        String owner = bearer(family, family.owner());
        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal()).param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal()).param("page", "-1")
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void withoutAccessTheListIsNotFound() throws Exception {
        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.outsider())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherFamily, otherFamily.owner())))
                .andExpect(status().isNotFound());
    }

    @Test
    void contributionsCannotBeEditedOrDeleted() throws Exception {
        // CA-04.8: there is no such operation; the contribution stays
        String owner = bearer(family, family.owner());
        mockMvc.perform(delete("/api/goals/{id}/contributions/{contribution}", family.sharedGoal(), family.ownerContribution())
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(put("/api/goals/{id}/contributions/{contribution}", family.sharedGoal(), family.ownerContribution())
                        .header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1.00}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/goals/{id}/contributions", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].amount".formatted(family.ownerContribution())).value(100.00));
    }

    private void registerOnPrivateGoal(String amount, LocalDate date) throws Exception {
        mockMvc.perform(post("/api/goals/{id}/contributions", family.privateGoal())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": %s, \"contributedOn\": \"%s\"}".formatted(amount, date)))
                .andExpect(status().isCreated());
    }
}
