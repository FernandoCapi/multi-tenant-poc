package com.vesta.goal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.vesta.support.ApiIntegrationTest;
import com.vesta.support.FamilyScenario;
import com.vesta.tenant.TenantContext;

/** Status transitions, editing and deletion (D-08, D-14, D-16, INV-11). */
class GoalLifecycleIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private FamilyScenario family;
    private String owner;

    @BeforeEach
    void createFamily() throws Exception {
        family = FamilyScenario.create("Ciclo");
        owner = bearer(family, family.owner());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void ownerCompletesReopensArchivesAndUnarchives() throws Exception {
        transition("complete").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        transition("reopen").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        transition("archive").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        transition("unarchive").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.archivedAt").isEmpty());
    }

    @Test
    void invalidTransitionsConflict() throws Exception {
        transition("reopen").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Só objetivos concluídos podem ser reabertos."));
        transition("unarchive").andExpect(status().isConflict());
        transition("complete").andExpect(status().isOk());
        transition("complete").andExpect(status().isConflict());
    }

    @Test
    void detailsChangeOnlyWhileActive() throws Exception {
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Viagem para a praia\", \"targetAmount\": 1500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Viagem para a praia"))
                .andExpect(jsonPath("$.targetAmount").value(1500.00));

        transition("complete").andExpect(status().isOk());
        mockMvc.perform(patch("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Outro\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Só objetivos ativos podem ser editados."));
    }

    @Test
    void goalWithoutContributionsIsDeletedAndThenNotFound() throws Exception {
        mockMvc.perform(delete("/api/goals/{id}", family.privateGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/goals/{id}", family.privateGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void goalWithContributionsCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/goals/{id}", family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Objetivos que já receberam contribuições não podem ser excluídos. Arquive o objetivo."));
    }

    @Test
    void concurrentChangeOfTheSameGoalFailsInsteadOfOverwriting() {
        TenantContext.set(family.familyId());

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Goal goal = goalRepository.findById(family.sharedGoal()).orElseThrow();
            bumpVersionInAnotherTransaction();
            goal.complete(Instant.now());
            goalRepository.flush();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    private org.springframework.test.web.servlet.ResultActions transition(String action) throws Exception {
        return mockMvc.perform(post("/api/goals/{id}/" + action, family.sharedGoal()).header(HttpHeaders.AUTHORIZATION, owner));
    }

    private void bumpVersionInAnotherTransaction() {
        try (Connection connection = connectAsOwner()) {
            inTenantTransaction(connection, family.familyId(), c -> {
                try (PreparedStatement statement =
                             c.prepareStatement("update goals set version = version + 1 where id = ?")) {
                    statement.setObject(1, family.sharedGoal());
                    return statement.executeUpdate();
                }
            });
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
