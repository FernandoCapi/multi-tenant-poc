package com.vesta.goal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/** Membership management (D-01, D-05..D-07, INV-04, CA-03.2..CA-03.7). */
class GoalMembershipIntegrationTest extends ApiIntegrationTest {

    private FamilyScenario family;
    private FamilyScenario otherFamily;

    @BeforeEach
    void createFamilies() throws Exception {
        family = FamilyScenario.create("A");
        otherFamily = FamilyScenario.create("B");
    }

    @Test
    void ownerAddsAFamilyMemberWhoThenSeesTheGoal() throws Exception {
        add(family.owner(), family.outsider(), "MEMBER", true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(4)));

        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, bearer(family, family.outsider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRole").value("MEMBER"));
    }

    @Test
    void addingTheSameMemberTwiceConflicts() throws Exception {
        add(family.owner(), family.member(), "MEMBER", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Esta pessoa já é membro do objetivo."));
    }

    @Test
    void userOfAnotherFamilyCannotBeAdded() throws Exception {
        // CA-03.4: same answer as a nonexistent id, revealing nothing about the other family
        add(family.owner(), otherFamily.owner(), "MEMBER", true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Membro da família não encontrado."));
        add(family.owner(), UUID.randomUUID(), "MEMBER", true)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Membro da família não encontrado."));
    }

    @Test
    void onlyOwnersManageMembers() throws Exception {
        add(family.member(), family.outsider(), "MEMBER", true).andExpect(status().isForbidden());
        add(family.outsider(), family.outsider(), "MEMBER", true).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.viewer())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.member())))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/goals/{id}/members/{user}", family.sharedGoal(), family.viewer())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.member()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contributes\": true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerChangesRoleAndParticipationSeparately() throws Exception {
        mockMvc.perform(patch("/api/goals/{id}/members/{user}", family.sharedGoal(), family.viewer())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contributes\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.userId == '%s')].contributes".formatted(family.viewer())).value(true))
                .andExpect(jsonPath("$.members[?(@.userId == '%s')].role".formatted(family.viewer())).value("MEMBER"));

        mockMvc.perform(patch("/api/goals/{id}/members/{user}", family.sharedGoal(), family.viewer())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void theLastOwnerCannotLeaveNorBeDemoted() throws Exception {
        String owner = bearer(family, family.owner());
        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.owner())
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O objetivo precisa de pelo menos um dono."));
        mockMvc.perform(patch("/api/goals/{id}/members/{user}", family.sharedGoal(), family.owner())
                        .header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"MEMBER\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void withAnotherOwnerTheOriginalOwnerMayLeave() throws Exception {
        String owner = bearer(family, family.owner());
        mockMvc.perform(patch("/api/goals/{id}/members/{user}", family.sharedGoal(), family.member())
                        .header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"OWNER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.owner())
                        .header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberLeavesLosesAccessAndKeepsTheirContributions() throws Exception {
        String member = bearer(family, family.member());
        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.member())
                        .header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, member))
                .andExpect(status().isNotFound());
        assertThat(contributionsOf(family.member())).isEqualTo(2);
    }

    @Test
    void removingSomeoneWhoIsNotAMemberIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/goals/{id}/members/{user}", family.sharedGoal(), family.outsider())
                        .header(HttpHeaders.AUTHORIZATION, bearer(family, family.owner())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Membro não encontrado neste objetivo."));
    }

    @Test
    void familyDirectoryListsOnlyTheTokenFamily() throws Exception {
        mockMvc.perform(get("/api/family/members").header(HttpHeaders.AUTHORIZATION, bearer(family, family.outsider())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(family.owner().toString(),
                        family.member().toString(), family.viewer().toString(), family.outsider().toString())));
    }

    private ResultActions add(UUID actor, UUID newMember, String role, boolean contributes) throws Exception {
        return mockMvc.perform(post("/api/goals/{id}/members", family.sharedGoal())
                .header(HttpHeaders.AUTHORIZATION, bearer(family, actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"userId": "%s", "role": "%s", "contributes": %s}""".formatted(newMember, role, contributes)));
    }

    private long contributionsOf(UUID contributor) throws Exception {
        try (Connection connection = connectAsOwner()) {
            return inTenantTransaction(connection, family.familyId(), c -> {
                try (PreparedStatement statement =
                             c.prepareStatement("select count(*) from contributions where contributor_id = ?")) {
                    statement.setObject(1, contributor);
                    try (ResultSet rows = statement.executeQuery()) {
                        rows.next();
                        return rows.getLong(1);
                    }
                }
            });
        }
    }
}
