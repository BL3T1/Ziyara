package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.domain.catalog.PlatformOrgGroups;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Ensures {@code sys_groups} Z1–Z7 and system {@code sys_roles} rows match the platform catalog after migrations.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.rbac-catalog-validation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformRbacCatalogValidator implements ApplicationRunner {

    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (PlatformOrgGroups.PlatformGroupSlice slice : PlatformOrgGroups.slices()) {
            Group g = groupRepository.findById(slice.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Platform org group missing: id=" + slice.id() + " code=" + slice.code()));
            if (!Objects.equals(slice.code(), g.getCode())) {
                throw new IllegalStateException(
                        "Platform org group code mismatch for id=" + slice.id() + ": expected " + slice.code()
                                + " but was " + g.getCode());
            }
        }

        for (UserRole ur : PlatformOrgGroups.allCatalogUserRoles()) {
            UUID expectedGroupId = PlatformOrgGroups.expectedGroupIdForUserRole(ur);
            Role r = roleRepository.findByCode(ur.name())
                    .orElseThrow(() -> new IllegalStateException(
                            "System role row missing for UserRole " + ur.name()));
            if (!Objects.equals(expectedGroupId, r.getGroupId())) {
                throw new IllegalStateException(
                        "System role " + ur.name() + " has group_id " + r.getGroupId() + " but catalog expects "
                                + expectedGroupId);
            }
        }

        log.info("Platform RBAC catalog validation passed (Z1–Z7 groups and system role group links).");
    }
}
