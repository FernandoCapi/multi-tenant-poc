package com.vesta.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

/**
 * Family member with a login. Read-only for the application: users come from seeds (D-11) and the
 * password hash is not mapped, because app_user cannot read it (ADR-0001).
 */
@Entity
@Immutable
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
