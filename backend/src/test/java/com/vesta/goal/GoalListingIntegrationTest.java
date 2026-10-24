package com.vesta.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/** Listing and creation respect the per-goal access and the token's family (CA-03.1, CA-03.8, D-15, RS-04). */
class GoalListingIntegrationTest extends ApiIntegrationTest {

    private FamilyScenario family;
    private FamilyScenario otherFamily;

    @BeforeEach
    void createFamilies() throws Exception {
        family = FamilyScenario.create("A");
        otherFamily = FamilyScenario.create("B");
    }

    @Test
    void eachUserListsOnlyTheGoalsTheyAreLinkedTo() throws Exception {
        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        family.sharedGoal().toString(), family.privateGoal().toString())))
                .andExpect(jsonPath("$[?(@.id == '%s')].myRole".formatted(family.sharedGoal())).value("OWNER"));

        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.viewer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(family.sharedGoal().toString())))
                .andExpect(jsonPath("$[0].myRole").value("MEMBER"))
                .andExpect(jsonPath("$[0].iContribute").value(false));
    }

    @Test
    void familyMemberWithoutLinkSeesNoGoalAtAll() throws Exception {
        // N-4: same family, but no membership in any goal
        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.outsider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void otherFamilySeesOnlyItsOwnGoals() throws Exception {
        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(otherFamily, otherFamily.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        otherFamily.sharedGoal().toString(), otherFamily.privateGoal().toString())));
    }

    @Test
    void archivedGoalsAreHiddenByDefaultAndListedOnRequest() throws Exception {
        String owner = bearer(family, family.owner());
        mockMvc.perform(post("/api/goals/{id}/archive", family.privateGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/goals").header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(family.sharedGoal().toString())));
        mockMvc.perform(get("/api/goals").param("status", "ARCHIVED").header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(family.privateGoal().toString())));
        mockMvc.perform(get("/api/goals/{id}", family.privateGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isOk());
    }

    @Test
    void createdGoalBelongsToTheTokenFamilyEvenWhenTheBodyNamesAnother() throws Exception {
        // N-2 through the API: the forged familyId is ignored
        String body = """
                {"title": "Reserva", "targetAmount": 500.00, "familyId": "%s", "family_id": "%s"}"""
                .formatted(otherFamily.familyId(), otherFamily.familyId());
        String response = mockMvc.perform(post("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.member()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.iContribute").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID goalId = UUID.fromString(JsonPath.read(response, "$.id"));

        assertThat(storedFamilyOf(goalId)).isEqualTo(family.familyId());
        mockMvc.perform(get("/api/goals/{id}", goalId).header(HttpHeaders.AUTHORIZATION, bearer(otherFamily, otherFamily.owner())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithMembersOfTheFamilyLinksThem() throws Exception {
        String body = """
                {"title": "Presente da vovó", "targetAmount": 300.00,
                 "members": [{"userId": "%s", "role": "MEMBER", "contributes": false}]}"""
                .formatted(family.outsider());
        String response = mockMvc.perform(post("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        String goalId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/goals/{id}", goalId).header(HttpHeaders.AUTHORIZATION, bearer(family, family.outsider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.iContribute").value(false));
    }

    @Test
    void memberOfAnotherFamilyCannotBeAddedOnCreation() throws Exception {
        String body = """
                {"title": "Tentativa", "targetAmount": 100.00,
                 "members": [{"userId": "%s", "role": "MEMBER"}]}""".formatted(otherFamily.owner());
        mockMvc.perform(post("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Membro da família não encontrado."));
    }

    @Test
    void invalidGoalIsRejected() throws Exception {
        mockMvc.perform(post("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \" \", \"targetAmount\": 0}"))
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(post("/api/goals").header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Centavos\", \"targetAmount\": 10.001}"))
                .andExpect(status().isUnprocessableContent());
    }

    private UUID storedFamilyOf(UUID goalId) throws Exception {
        try (Connection connection = connectAsOwner()) {
            return inTenantTransaction(connection, family.familyId(), c -> {
                try (PreparedStatement statement = c.prepareStatement("select family_id from goals where id = ?")) {
                    statement.setObject(1, goalId);
                    try (ResultSet rows = statement.executeQuery()) {
                        return rows.next() ? rows.getObject(1, UUID.class) : null;
                    }
                }
            });
        }
    }
}
