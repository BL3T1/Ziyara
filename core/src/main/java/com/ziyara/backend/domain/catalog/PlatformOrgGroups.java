package com.ziyara.backend.domain.catalog;

import com.ziyara.backend.domain.enums.UserRole;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stable platform organizational groups ({@code sys_groups}): codes {@code Z1}–{@code Z7}
 * and fixed UUIDs from seed/migrations. Custom admin-created groups must not use this pattern.
 */
public final class PlatformOrgGroups {

    private PlatformOrgGroups() {}

    /** Reserved for platform: one {@code Z} followed by one or more digits (stored uppercase). */
    public static boolean isReservedPlatformGroupCode(String code) {
        return code != null && code.matches("^Z[0-9]+$");
    }

    /** Seeded platform slice (Z1–Z7); these rows must not be deleted or renamed away via custom-group rules. */
    public static boolean isPlatformGroupId(UUID id) {
        if (id == null) {
            return false;
        }
        for (PlatformGroupSlice s : SLICES) {
            if (s.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public record PlatformGroupSlice(UUID id, String code, int sortIndex, Set<UserRole> systemRoles) {}

    private static final List<PlatformGroupSlice> SLICES = List.of(
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000001"), "Z1", 1,
                    EnumSet.of(UserRole.SUPER_ADMIN, UserRole.CEO, UserRole.GENERAL_MANAGER)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000002"), "Z2", 2,
                    EnumSet.of(UserRole.SALES_MANAGER, UserRole.SALES_REPRESENTATIVE)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000003"), "Z3", 3,
                    EnumSet.of(UserRole.FINANCE_MANAGER, UserRole.ACCOUNTANT)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000004"), "Z4", 4,
                    EnumSet.of(UserRole.SUPPORT_MANAGER, UserRole.SUPPORT_AGENT)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000005"), "Z5", 5,
                    EnumSet.of(UserRole.HR_MANAGER)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000006"), "Z6", 6,
                    EnumSet.of(UserRole.PROVIDER_MANAGER, UserRole.PROVIDER_FINANCE, UserRole.PROVIDER_STAFF, UserRole.TAXI_OPERATOR)),
            new PlatformGroupSlice(UUID.fromString("b0000000-0000-0000-0000-000000000007"), "Z7", 7,
                    EnumSet.of(UserRole.CUSTOMER))
    );

    private static final Map<UserRole, UUID> USER_ROLE_TO_GROUP_ID = buildUserRoleToGroupId();

    private static Map<UserRole, UUID> buildUserRoleToGroupId() {
        Map<UserRole, UUID> m = new EnumMap<>(UserRole.class);
        for (PlatformGroupSlice s : SLICES) {
            for (UserRole ur : s.systemRoles()) {
                if (m.put(ur, s.id()) != null) {
                    throw new IllegalStateException("Duplicate UserRole in platform catalog: " + ur);
                }
            }
        }
        return Collections.unmodifiableMap(m);
    }

    public static List<PlatformGroupSlice> slices() {
        return SLICES;
    }

    public static UUID expectedGroupIdForUserRole(UserRole userRole) {
        return USER_ROLE_TO_GROUP_ID.get(userRole);
    }

    public static Set<UserRole> allCatalogUserRoles() {
        return Arrays.stream(UserRole.values())
                .filter(USER_ROLE_TO_GROUP_ID::containsKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
