package com.vesta.goal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;

/**
 * Link between a user and a goal. The role defines access; {@code contributes} defines financial
 * participation (D-05). Only linked users see the goal (INV-03).
 */
@Entity
@IdClass(GoalMemberId.class)
@Table(name = "goal_members")
public class GoalMember {

    @TenantId
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Id
    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalRole role;

    @Column(nullable = false)
    private boolean contributes;

    @Column(name = "added_by", nullable = false, updatable = false)
    private UUID addedBy;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    protected GoalMember() {
    }

    public GoalMember(UUID goalId, UUID userId, GoalRole role, boolean contributes, UUID addedBy, Instant addedAt) {
        this.goalId = Objects.requireNonNull(goalId, "goalId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.role = Objects.requireNonNull(role, "role");
        this.contributes = contributes;
        this.addedBy = Objects.requireNonNull(addedBy, "addedBy");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt");
    }

    public boolean isOwner() {
        return role == GoalRole.OWNER;
    }

    public void changeRole(GoalRole newRole) {
        role = Objects.requireNonNull(newRole, "newRole");
    }

    public void changeContributes(boolean newContributes) {
        contributes = newContributes;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getUserId() {
        return userId;
    }

    public GoalRole getRole() {
        return role;
    }

    public boolean isContributes() {
        return contributes;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
