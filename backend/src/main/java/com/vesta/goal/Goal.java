package com.vesta.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "goals")
public class Goal {

    public static final String DEFAULT_CURRENCY = "BRL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Filled and filtered by Hibernate from the bound tenant; never set from client input. */
    @TenantId
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    // Single currency for now (D-12), but loaded from the column by Hibernate through reflection:
    // the field cannot be final (JPA forbids final persistent fields) nor a local variable.
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    @Column(nullable = false, length = 3)
    private String currency = DEFAULT_CURRENCY;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // Not final: JPA forbids final persistent fields and Hibernate sets it on load. Remove the
    // suppression once the status transitions (D-08) change this field.
    @SuppressWarnings("FieldMayBeFinal")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Goal() {
    }

    public Goal(String title, BigDecimal targetAmount, UUID createdBy) {
        this.title = title;
        this.targetAmount = targetAmount;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
