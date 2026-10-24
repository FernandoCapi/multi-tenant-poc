package com.vesta.contribution;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;

/**
 * Contribution permissions on the shared goal (docs/requisitos.md §7, D-03, D-05, D-07; scenarios
 * N-1, N-4, N-6): 404 for whoever cannot see the goal, 403 for whoever sees it without permission.
 */
class ContributionAuthorizationIntegrationTest extends ApiIntegrationTest {

    enum Actor { OWNER, MEMBER, VIEWER, OUTSIDER, OTHER_FAMILY }

    enum Action { REGISTER_OWN, REGISTER_FOR_OTHER }

    private FamilyScenario family;
    private FamilyScenario otherFamily;

    @BeforeEach
    void createFamilies() throws Exception {
        family = FamilyScenario.create("A");
        otherFamily = FamilyScenario.create("B");
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                Arguments.of(Actor.OWNER, Action.REGISTER_OWN, 201),
                Arguments.of(Actor.MEMBER, Action.REGISTER_OWN, 201),
                // Sees the goal but does not participate financially (N-6)
                Arguments.of(Actor.VIEWER, Action.REGISTER_OWN, 403),
                Arguments.of(Actor.OUTSIDER, Action.REGISTER_OWN, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.REGISTER_OWN, 404),
                Arguments.of(Actor.OWNER, Action.REGISTER_FOR_OTHER, 201),
                Arguments.of(Actor.MEMBER, Action.REGISTER_FOR_OTHER, 403),
                Arguments.of(Actor.VIEWER, Action.REGISTER_FOR_OTHER, 403),
                Arguments.of(Actor.OUTSIDER, Action.REGISTER_FOR_OTHER, 404),
                Arguments.of(Actor.OTHER_FAMILY, Action.REGISTER_FOR_OTHER, 404));
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @MethodSource("matrix")
    void permissionMatrix(Actor actor, Action action, int expectedStatus) throws Exception {
        mockMvc.perform(request(actor, action)).andExpect(status().is(expectedStatus));
    }

    private MockHttpServletRequestBuilder request(Actor actor, Action action) {
        UUID otherParticipant = actor == Actor.OWNER ? family.member() : family.owner();
        String contributor = action == Action.REGISTER_FOR_OTHER ? "\"" + otherParticipant + "\"" : "null";
        return post("/api/goals/{id}/contributions", family.sharedGoal())
                .header(HttpHeaders.AUTHORIZATION, tokenOf(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 25.00, \"contributorId\": " + contributor + "}");
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
