package com.vesta.tenant;

import java.util.UUID;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Feeds Hibernate's {@code @TenantId} discriminator from {@link TenantContext}.
 * Without a bound tenant it returns {@link #NO_TENANT}, which matches no family, so
 * Hibernate-generated queries fail closed instead of running unfiltered.
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    public static final UUID NO_TENANT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.get().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
