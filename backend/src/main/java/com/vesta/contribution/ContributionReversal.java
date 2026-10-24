package com.vesta.contribution;

import java.time.Instant;
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

/** Cancels one contribution, with author, time and reason; at most one per contribution (D-13). */
@Entity
@Immutable
@Table(name = "contribution_reversals")
public class ContributionReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "contribution_id", nullable = false, updatable = false)
    private UUID contributionId;

    @Column(name = "reversed_by_id", nullable = false, updatable = false)
    private UUID reversedById;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "reversed_at", nullable = false, updatable = false)
    private Instant reversedAt;

    protected ContributionReversal() {
    }

    public ContributionReversal(UUID contributionId, UUID reversedById, String reason, Instant reversedAt) {
        this.contributionId = Objects.requireNonNull(contributionId, "contributionId");
        this.reversedById = Objects.requireNonNull(reversedById, "reversedById");
        this.reason = Objects.requireNonNull(reason, "reason");
        this.reversedAt = Objects.requireNonNull(reversedAt, "reversedAt");
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getContributionId() {
        return contributionId;
    }

    public UUID getReversedById() {
        return reversedById;
    }

    public String getReason() {
        return reason;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }
}
