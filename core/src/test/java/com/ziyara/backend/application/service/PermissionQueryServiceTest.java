package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.PermissionSummaryResponse;
import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionQueryServiceTest {

    @Mock PermissionRepository permissionRepository;

    PermissionQueryService service;

    @BeforeEach
    void setUp() {
        service = new PermissionQueryService(permissionRepository);
    }

    // ── getPermissionCatalogue ────────────────────────────────────────────────

    @Test
    void getPermissionCatalogue_mapsAllPermissions() {
        Permission perm = permission("bookings:read", "bookings", "read", false);
        when(permissionRepository.findAll()).thenReturn(List.of(perm));

        List<PermissionSummaryResponse> result = service.getPermissionCatalogue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("bookings:read");
        assertThat(result.get(0).getResource()).isEqualTo("bookings");
        assertThat(result.get(0).getAction()).isEqualTo("read");
        assertThat(result.get(0).isLocked()).isFalse();
    }

    @Test
    void getPermissionCatalogue_emptyRepo_returnsEmpty() {
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertThat(service.getPermissionCatalogue()).isEmpty();
    }

    @Test
    void getPermissionCatalogue_includesLockedPermissions() {
        Permission locked = permission("system:admin", "system", "admin", true);
        when(permissionRepository.findAll()).thenReturn(List.of(locked));

        List<PermissionSummaryResponse> result = service.getPermissionCatalogue();

        assertThat(result.get(0).isLocked()).isTrue();
    }

    // ── getUnlockedPermissions ────────────────────────────────────────────────

    @Test
    void getUnlockedPermissions_returnsOnlyUnlocked() {
        Permission unlocked = permission("bookings:read", "bookings", "read", false);
        when(permissionRepository.findAllUnlocked()).thenReturn(List.of(unlocked));

        List<PermissionSummaryResponse> result = service.getUnlockedPermissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLocked()).isFalse();
    }

    @Test
    void getUnlockedPermissions_emptyResult_returnsEmpty() {
        when(permissionRepository.findAllUnlocked()).thenReturn(List.of());

        assertThat(service.getUnlockedPermissions()).isEmpty();
    }

    private Permission permission(String code, String resource, String action, boolean locked) {
        Permission p = new Permission();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setName(code);
        p.setResource(resource);
        p.setAction(action);
        p.setLocked(locked);
        return p;
    }
}
