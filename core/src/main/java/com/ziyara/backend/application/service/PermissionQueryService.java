package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.PermissionSummaryResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionQueryService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "permissionCatalogue", key = "'all'")
    public List<PermissionSummaryResponse> getPermissionCatalogue() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionSummaryResponse> getUnlockedPermissions() {
        return permissionRepository.findAllUnlocked().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toList());
    }

    public PermissionSummaryResponse toPermissionSummary(Permission p) {
        return PermissionSummaryResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(RequestLocaleHolder.localized(p.getName(), p.getNameAr()))
                .resource(p.getResource())
                .action(p.getAction())
                .locked(p.isLocked())
                .build();
    }
}
