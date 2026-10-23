package com.vesta.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
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

/**
 * Financial goal. Status rules (D-08, D-14, D-16): details change only while ACTIVE; the owner
 * completes, reopens, archives and unarchives; reaching the target never changes the status.
 */
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

    public Goal(String title, String description, BigDecimal targetAmount, LocalDate dueDate,
            UUID createdBy, Instant createdAt) {
        this.title = Objects.requireNonNull(title, "title");
        this.description = description;
        this.targetAmount = Objects.requireNonNull(targetAmount, "targetAmount");
        this.dueDate = dueDate;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Replaces only the given values; null keeps the current one (D-16: ACTIVE only). */
    public void updateDetails(String newTitle, String newDescription, BigDecimal newTargetAmount,
            LocalDate newDueDate) {
        requireStatus(GoalStatus.ACTIVE, "Só objetivos ativos podem ser editados.");
        if (newTitle != null) {
            title = newTitle;
        }
        if (newDescription != null) {
            description = newDescription;
        }
        if (newTargetAmount != null) {
            targetAmount = newTargetAmount;
        }
        if (newDueDate != null) {
            dueDate = newDueDate;
        }
    }

    public void complete(Instant now) {
        requireStatus(GoalStatus.ACTIVE, "Só objetivos ativos podem ser concluídos.");
        status = GoalStatus.COMPLETED;
        completedAt = now;
    }

    public void reopen() {
        requireStatus(GoalStatus.COMPLETED, "Só objetivos concluídos podem ser reabertos.");
        status = GoalStatus.ACTIVE;
        completedAt = null;
    }

    public void archive(Instant now) {
        if (status == GoalStatus.ARCHIVED) {
            throw new GoalStateConflictException("O objetivo já está arquivado.");
        }
        status = GoalStatus.ARCHIVED;
        archivedAt = now;
    }

    /** An unarchived goal is active again and may receive contributions (D-14). */
    public void unarchive() {
        requireStatus(GoalStatus.ARCHIVED, "Só objetivos arquivados podem ser desarquivados.");
        status = GoalStatus.ACTIVE;
        archivedAt = null;
        completedAt = null;
    }

    private void requireStatus(GoalStatus expected, String conflictMessage) {
        if (status != expected) {
            throw new GoalStateConflictException(conflictMessage);
        }
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

    public long getVersion() {
        return version;
    }
}
