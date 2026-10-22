package com.vesta.tenant;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Family (tenant) bound to the current request thread.
 * It is only ever filled from server-side identity, never from client input (RS-04), and must
 * be cleared at the end of the request. It is not propagated to other threads.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_FAMILY = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID familyId) {
        CURRENT_FAMILY.set(Objects.requireNonNull(familyId, "familyId"));
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT_FAMILY.get());
    }

    public static UUID require() {
        return get().orElseThrow(() -> new IllegalStateException("No tenant bound to the current thread"));
    }

    public static void clear() {
        CURRENT_FAMILY.remove();
    }
}
