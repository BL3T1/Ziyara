package com.ziyara.backend.infrastructure.messaging;

import com.ziyara.backend.application.dto.StaffNotificationEvent;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves {@link StaffNotificationEvent#getNotifyRoles()} to active company user ids.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffNotificationRecipientResolver {

    private static final int MAX_RECIPIENTS = 500;

    private final UserRepository userRepository;

    public List<UUID> resolveRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (String raw : roleNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                UserRole role = UserRole.valueOf(raw.trim().toUpperCase());
                userRepository.findActiveDirectoryUserIdsByRoles(List.of(role), UserStatus.ACTIVE).forEach(ids::add);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown role in staff notification event: {}", raw);
            }
            if (ids.size() >= MAX_RECIPIENTS) {
                break;
            }
        }
        return new ArrayList<>(ids).subList(0, Math.min(ids.size(), MAX_RECIPIENTS));
    }
}
