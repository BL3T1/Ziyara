package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AddPortalStaffRequest;
import com.ziyara.backend.application.dto.request.CreatePortalStaffUserRequest;
import com.ziyara.backend.application.dto.request.UpdatePortalStaffRequest;
import com.ziyara.backend.application.dto.response.PortalStaffMemberResponse;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.infrastructure.persistence.entity.ProviderStaffJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import com.ziyara.backend.infrastructure.persistence.entity.UserJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.ProviderStaffJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.ServiceProviderJpaRepository;
import com.ziyara.backend.infrastructure.persistence.repository.UserJpaRepository;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalStaffService {

    private final ProviderStaffJpaRepository providerStaffJpaRepository;
    private final ServiceProviderJpaRepository serviceProviderJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRbacAssignmentService userRbacAssignmentService;

    @Transactional(readOnly = true)
    public List<PortalStaffMemberResponse> listStaff(UUID providerId) {
        ServiceProviderJpaEntity provider = serviceProviderJpaRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        List<PortalStaffMemberResponse> out = new ArrayList<>();
        UUID ownerId = provider.getCreatedBy();
        if (ownerId != null) {
            userJpaRepository.findById(ownerId).ifPresent(u -> out.add(mapUser(u, true, null, null, u.getCreatedAt())));
        }
        for (ProviderStaffJpaEntity link : providerStaffJpaRepository.findByProviderIdOrderByCreatedAtAsc(providerId)) {
            userJpaRepository.findById(link.getUserId()).ifPresent(u ->
                    out.add(mapUser(u, false, link.getId(), link.getTitle(), link.getCreatedAt())));
        }
        return out;
    }

    @Transactional
    public PortalStaffMemberResponse addStaff(UUID providerId, AddPortalStaffRequest request) {
        ServiceProviderJpaEntity provider = serviceProviderJpaRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getCreatedBy();
        if (ownerId != null && ownerId.equals(request.getUserId())) {
            throw new BusinessException("User is already the primary owner of this provider");
        }
        UserJpaEntity user = userJpaRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!isProviderPortalRole(user.getRole())) {
            throw new BusinessException("User must have a provider portal role");
        }
        serviceProviderJpaRepository.findByCreatedBy(request.getUserId()).ifPresent(other -> {
            if (!other.getId().equals(providerId)) {
                throw new BusinessException("User is the owner of another provider");
            }
        });
        providerStaffJpaRepository.findByUserId(request.getUserId()).ifPresent(link -> {
            if (!link.getProviderId().equals(providerId)) {
                throw new BusinessException("User is already linked to another provider");
            }
        });
        if (providerStaffJpaRepository.findByProviderIdAndUserId(providerId, request.getUserId()).isPresent()) {
            throw new BusinessException("User is already on this team");
        }
        ProviderStaffJpaEntity saved = providerStaffJpaRepository.save(ProviderStaffJpaEntity.builder()
                .providerId(providerId)
                .userId(request.getUserId())
                .title(trimToNull(request.getTitle()))
                .build());
        return mapUser(user, false, saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    @Transactional
    public PortalStaffMemberResponse createStaffUser(UUID providerId, UUID actorUserId, UserRole actorRole, CreatePortalStaffUserRequest request) {
        ServiceProviderJpaEntity provider = serviceProviderJpaRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        if (actorRole != UserRole.PROVIDER_MANAGER) {
            throw new BusinessException("Only provider managers can create provider staff users");
        }
        UUID ownerId = provider.getCreatedBy();
        boolean linkedMember = providerStaffJpaRepository.findByProviderIdAndUserId(providerId, actorUserId).isPresent();
        if (!actorUserId.equals(ownerId) && !linkedMember) {
            throw new BusinessException("You are not linked to this provider organization");
        }

        UserRole requestedRole = request.getRole();
        if (!isProviderPortalRole(requestedRole)) {
            throw new BusinessException("Role must be a provider portal role");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userJpaRepository.existsByEmail(email)) {
            throw new BusinessException("User with email already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userJpaRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("User with phone already exists");
        }

        UserJpaEntity user = UserJpaEntity.builder()
                .email(email)
                .phone(trimToNull(request.getPhone()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .status(UserStatus.ACTIVE)
                .build();
        user = userJpaRepository.save(user);
        userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(user.getId(), user.getRole());

        ProviderStaffJpaEntity saved = providerStaffJpaRepository.save(ProviderStaffJpaEntity.builder()
                .providerId(providerId)
                .userId(user.getId())
                .title(trimToNull(request.getTitle()))
                .build());
        return mapUser(user, false, saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    @Transactional
    public PortalStaffMemberResponse updateStaff(UUID providerId, UUID userId, UpdatePortalStaffRequest request) {
        ServiceProviderJpaEntity provider = serviceProviderJpaRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getCreatedBy();
        if (ownerId != null && ownerId.equals(userId)) {
            throw new BusinessException("Cannot update the owner via staff link; use profile instead");
        }
        ProviderStaffJpaEntity link = providerStaffJpaRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        if (request.getTitle() != null) {
            link.setTitle(trimToNull(request.getTitle()));
        }
        providerStaffJpaRepository.save(link);
        UserJpaEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapUser(user, false, link.getId(), link.getTitle(), link.getCreatedAt());
    }

    @Transactional
    public void removeStaff(UUID providerId, UUID userId) {
        ServiceProviderJpaEntity provider = serviceProviderJpaRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getCreatedBy();
        if (ownerId != null && ownerId.equals(userId)) {
            throw new BusinessException("Cannot remove the primary owner from the provider");
        }
        ProviderStaffJpaEntity link = providerStaffJpaRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        providerStaffJpaRepository.delete(link);
    }

    private static PortalStaffMemberResponse mapUser(
            UserJpaEntity u,
            boolean owner,
            UUID staffLinkId,
            String title,
            java.time.LocalDateTime createdAt
    ) {
        return PortalStaffMemberResponse.builder()
                .staffLinkId(staffLinkId)
                .userId(u.getId())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .title(title)
                .owner(owner)
                .createdAt(createdAt)
                .build();
    }

    private static boolean isProviderPortalRole(UserRole role) {
        return role == UserRole.PROVIDER_MANAGER
                || role == UserRole.PROVIDER_FINANCE
                || role == UserRole.PROVIDER_STAFF
                || role == UserRole.TAXI_OPERATOR;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
