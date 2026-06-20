package com.ziyara.backend.infrastructure.rls;

import java.util.UUID;

/**
 * Per-request attributes mirrored into PostgreSQL session GUCs for RLS policies.
 *
 * @param rlsBypass when true, policies treat the session as privileged (company staff / jobs).
 * @param currentUserId authenticated subject (JWT sub), when known
 * @param currentProviderId provider scope from JWT {@code pid} (owner or staff home provider)
 */
public record RlsSessionAttributes(boolean rlsBypass, UUID currentUserId, UUID currentProviderId) {

    /**
     * Anonymous / pre-auth threads: do not tighten RLS (policies default-open when GUCs unset).
     */
    public static RlsSessionAttributes open() {
        return new RlsSessionAttributes(true, null, null);
    }
}
