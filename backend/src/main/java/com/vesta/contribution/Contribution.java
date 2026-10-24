package com.vesta.contribution;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/**
 * Money actually saved or paid towards a goal (D-02). Insert-only, like the database grants: a
 * mistake is corrected with a {@link ContributionReversal} and a new contribution, never an update.
 */
@Entity
@Immutable
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Column(name = "contributor_id", nullable = false, updatable = false)
    private UUID contributorId;

    @Column(name = "registered_by_id", nullable = false, updatable = false)
    private UUID registeredById;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "contributed_on", nullable = false, updatable = false)
    private LocalDate contributedOn;

    @Column(length = 500, updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Contribution() {
    }

    public Contribution(UUID goalId, UUID contributorId, UUID registeredById, BigDecimal amount,
            LocalDate contributedOn, String note, Instant createdAt) {
        this.goalId = Objects.requireNonNull(goalId, "goalId");
        this.contributorId = Objects.requireNonNull(contributorId, "contributorId");
        this.registeredById = Objects.requireNonNull(registeredById, "registeredById");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.contributedOn = Objects.requireNonNull(contributedOn, "contributedOn");
        this.note = note;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getContributorId() {
        return contributorId;
    }

    public UUID getRegisteredById() {
        return registeredById;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getContributedOn() {
        return contributedOn;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
