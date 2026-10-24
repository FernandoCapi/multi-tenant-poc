package com.vesta.goal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/**
 * Permission matrix of docs/requisitos.md §7 on the shared goal (RS-01, RS-02, D-09):
 * 404 for whoever cannot see the goal, 403 for whoever sees it without permission.
 * Demonstration scenarios N-1 (other family), N-4 (same family, no link) and N-5 (MEMBER).
 */
class GoalAuthorizationIntegrationTest extends ApiIntegrationTest {

    enum Actor { OWNER, MEMBER, VIEWER, OUTSIDER, OTHER_FAMILY }

    enum Action {
        VIEW(goalId -> get("/api/goals/{id}", goalId)),
        UPDATE(goalId -> patch("/api/goals/{id}", goalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Novo título\"}")),
        COMPLETE(goalId -> post("/api/goals/{id}/complete", goalId)),
        ARCHIVE(goalId -> post("/api/goals/{id}/archive", goalId)),
        DELETE(goalId -> delete("/api/goals/{id}", goalId));

        private final Function<UUID, MockHttpServletRequestBuilder> request;

        Action(Function<UUID, MockHttpServletRequestBuilder> request) {
            this.request = request;
        }
    }

    private FamilyScenario family;
    private FamilyScenario otherFamily;

    @BeforeEach
    void createFamilies() throws Exception {
        family = FamilyScenario.create("A");
        otherFamily = FamilyScenario.create("B");
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                // VIEW: everyone linked sees the goal
                Arguments.of(Actor.OWNER, Action.VIEW, 200),
                Arguments.of(Actor.MEMBER, Action.VIEW, 200),
                Arguments.of(Actor.VIEWER, Action.VIEW, 200),
                Arguments.of(Actor.OUTSIDER, Action.VIEW, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.VIEW, 404),
                // Owner actions
                Arguments.of(Actor.OWNER, Action.UPDATE, 200),
                Arguments.of(Actor.MEMBER, Action.UPDATE, 403),
                Arguments.of(Actor.VIEWER, Action.UPDATE, 403),
                Arguments.of(Actor.OUTSIDER, Action.UPDATE, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.UPDATE, 404),
                Arguments.of(Actor.OWNER, Action.COMPLETE, 200),
                Arguments.of(Actor.MEMBER, Action.COMPLETE, 403),
                Arguments.of(Actor.VIEWER, Action.COMPLETE, 403),
                Arguments.of(Actor.OUTSIDER, Action.COMPLETE, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.COMPLETE, 404),
                Arguments.of(Actor.OWNER, Action.ARCHIVE, 200),
                Arguments.of(Actor.MEMBER, Action.ARCHIVE, 403),
                Arguments.of(Actor.VIEWER, Action.ARCHIVE, 403),
                Arguments.of(Actor.OUTSIDER, Action.ARCHIVE, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.ARCHIVE, 404),
                // The shared goal has contributions: even its owner cannot delete it (409, INV-11)
                Arguments.of(Actor.OWNER, Action.DELETE, 409),
                Arguments.of(Actor.MEMBER, Action.DELETE, 403),
                Arguments.of(Actor.VIEWER, Action.DELETE, 403),
                Arguments.of(Actor.OUTSIDER, Action.DELETE, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.DELETE, 404));
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @MethodSource("matrix")
    void permissionMatrixOnTheSharedGoal(Actor actor, Action action, int expectedStatus) throws Exception {
        mockMvc.perform(action.request.apply(family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(actor)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void notFoundAndForbiddenBodiesRevealNothingAboutTheGoal() throws Exception {
        mockMvc.perform(get("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.OTHER_FAMILY)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Objetivo não encontrado."));
        mockMvc.perform(get("/api/goals/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.OWNER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Objetivo não encontrado."));
        mockMvc.perform(post("/api/goals/{id}/complete", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.MEMBER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para esta ação."));
    }

    @Test
    void visibilityAndPermissionAreCheckedBeforeTheRequestBody() throws Exception {
        String invalidBody = "{\"targetAmount\": -5}";
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.OUTSIDER))
                        .contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, tokenOf(Actor.OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isUnprocessableContent());
    }

    private String tokenOf(Actor actor) {
        return switch (actor) {
            case OWNER -> bearer(family, family.owner());
            case MEMBER -> bearer(family, family.member());
            case VIEWER -> bearer(family, family.viewer());
            case OUTSIDER -> bearer(family, family.outsider());
            case OTHER_FAMILY -> bearer(otherFamily, otherFamily.owner());
        };
    }
}
