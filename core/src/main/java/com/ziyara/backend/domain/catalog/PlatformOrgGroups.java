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
 * Stable platform organizational group ({@code sys_groups}): code {@code C1} with fixed UUID.
 * Custom admin-created groups must not use C-followed-by-digits codes.
 */
public final class PlatformOrgGroups {

    private PlatformOrgGroups() {}

    /** Reserved for platform: {@code C} followed by one or more digits (stored uppercase). */
    public static boolean isReservedPlatformGroupCode(String code) {
        return code != null && code.matches("^C[0-9]+$");
    }

    /** Returns true if the given UUID is the C1 Admin platform group. */
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
            new PlatformGroupSlice(
                    UUID.fromString("b0000000-0000-0000-0000-000000000010"),
                    "C1",
                    1,
                    EnumSet.of(UserRole.SUPER_ADMIN))
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
