package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceProviderRequest;
import com.ziyara.backend.application.dto.request.UpdateProviderCommissionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceProviderRequest;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service provider management.
 * <p>Product rules: Super Admin / CEO creates partners as {@link ProviderStatus#ACTIVE} with portal login
 * {@link UserStatus#ACTIVE}. Sales roles create {@link ProviderStatus#PENDING_APPROVAL} and keep the linked
 * {@link UserRole#PROVIDER_MANAGER} in {@link UserStatus#PENDING_VERIFICATION} until Super Admin / CEO approves.
 * Rejection sets provider {@link ProviderStatus#INACTIVE} and linked manager {@link UserStatus#INACTIVE}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10");

    private final ServiceProviderRepository serviceProviderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ProviderWorkflowEmailService providerWorkflowEmailService;
    private final UserRbacAssignmentService userRbacAssignmentService;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    private static boolean isSalesCreator(UserRole role) {
        return role == UserRole.SALES_MANAGER || role == UserRole.SALES_REPRESENTATIVE;
    }

    private static boolean isElevatedCreator(UserRole role) {
        return role == UserRole.SUPER_ADMIN || role == UserRole.CEO;
    }

    /**
     * Creates a provider; activation vs pending is derived from the creator's role.
     */
    @Transactional
    public ServiceProviderResponse createProvider(CreateServiceProviderRequest request, UUID actorUserId, UserRole creatorRole) {
        if (!isSalesCreator(creatorRole) && !isElevatedCreator(creatorRole)) {
            throw new IllegalArgumentException("Your role cannot create provider accounts");
        }

        boolean mgrEmailOk = request.getManagerEmail() != null && !request.getManagerEmail().isBlank();
        boolean hasManagerPassword = request.getManagerPassword() != null && !request.getManagerPassword().isBlank();
        if (!mgrEmailOk) {
            throw new IllegalArgumentException("managerEmail is required");
        }
        if (hasManagerPassword && request.getManagerPassword().length() < 6) {
            throw new IllegalArgumentException("managerPassword must be at least 6 characters");
        }
        boolean createNewManager = hasManagerPassword;
        if (!createNewManager && request.getManagerPhone() != null && !request.getManagerPhone().isBlank()) {
            throw new IllegalArgumentException("managerPhone can only be provided when creating a new manager user");
        }

        if (!createNewManager && request.getEmail() != null && request.getEmail().isBlank()) {
            request.setEmail(null);
        }

        if (serviceProviderRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Provider with this name already exists");
        }

        if (request.getType() == null || request.getType().isBlank()) {
            throw new IllegalArgumentException("Provider type is required");
        }
        final ServiceType providerServiceType;
        try {
            providerServiceType = ServiceType.valueOf(request.getType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid provider type");
        }

        User managerUser;
        if (!createNewManager) {
            String managerEmail = request.getManagerEmail().trim().toLowerCase();
            managerUser = userRepository.findByEmail(managerEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + managerEmail));
            if (managerUser.getRole() != UserRole.PROVIDER_MANAGER) {
                throw new IllegalArgumentException("Linked user must have role PROVIDER_MANAGER");
            }
            if (serviceProviderRepository.findByUserId(managerUser.getId()).isPresent()) {
                throw new IllegalArgumentException("This user already has a provider profile");
            }
        } else {
            String email = request.getManagerEmail().trim().toLowerCase();
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("User with this email already exists");
            }
            if (request.getManagerPhone() != null && !request.getManagerPhone().isBlank()
                    && userRepository.existsByPhone(request.getManagerPhone())) {
                throw new IllegalArgumentException("User with this phone already exists");
            }
            managerUser = new User();
            managerUser.setEmail(email);
            managerUser.setPhone(request.getManagerPhone());
            managerUser.setPasswordHash(passwordEncoder.encode(request.getManagerPassword()));
            managerUser.setRole(UserRole.PROVIDER_MANAGER);
            boolean pendingPath = isSalesCreator(creatorRole);
            managerUser.setStatus(pendingPath ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE);
            managerUser = userRepository.save(managerUser);
            userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(managerUser.getId(), managerUser.getRole());
        }

        if (!createNewManager) {
            if (isSalesCreator(creatorRole)) {
                managerUser.setStatus(UserStatus.PENDING_VERIFICATION);
                userRepository.save(managerUser);
            } else {
                managerUser.setStatus(UserStatus.ACTIVE);
                userRepository.save(managerUser);
            }
        }

        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(managerUser.getId());
        provider.setName(request.getName());
        provider.setType(providerServiceType.name());
        provider.setRegistrationNumber(request.getRegistrationNumber());
        provider.setPhone(request.getPhone());
        String contactEmail = request.getEmail();
        if (contactEmail == null || contactEmail.isBlank()) {
            contactEmail = managerUser.getEmail();
        }
        provider.setEmail(contactEmail);
        provider.setAddress(request.getAddress());
        provider.setDescription(request.getDescription());
        if (request.getLogoUrl() != null && !request.getLogoUrl().isBlank()) {
            provider.setLogoUrl(request.getLogoUrl().trim());
        }
        provider.setRating(0.0);
        provider.setReviewCount(0);

        boolean pendingProvider = isSalesCreator(creatorRole);
        if (pendingProvider) {
            provider.setStatus(ProviderStatus.PENDING_APPROVAL);
            provider.setVerified(false);
        } else {
            provider.setStatus(ProviderStatus.ACTIVE);
            provider.setVerified(true);
            provider.setApprovedBy(actorUserId);
            provider.setApprovedAt(LocalDateTime.now());
        }

        ServiceProvider saved = serviceProviderRepository.save(provider);
        String action = pendingProvider ? "PROVIDER_SUBMIT_PENDING" : "PROVIDER_CREATE_ACTIVE";
        auditLogService.logAction(action, "ServiceProvider", saved.getId().toString(), actorUserId,
                null, "name=" + saved.getName() + ";userId=" + managerUser.getId(), null, null);
        if (pendingProvider) {
            providerWorkflowEmailService.notifySubmittedForApproval(saved, managerUser.getEmail());
            staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.PROVIDER_PENDING_REVIEW.name())
                    .title("Provider pending approval")
                    .message("Partner \"" + saved.getName() + "\" is waiting for approval.")
                    .notifyRoles(List.of("SALES_MANAGER", "SUPPORT_MANAGER", "CEO", "GENERAL_MANAGER"))
                    .metadata("{\"providerId\":\"" + saved.getId() + "\"}")
                    .build());
        } else {
            providerWorkflowEmailService.notifyActivated(saved, managerUser.getEmail());
        }
        log.info("{} provider {} by role {}", pendingProvider ? "Pending" : "Active", saved.getId(), creatorRole);
        return mapToResponse(saved);
    }

    @Transactional
    public ServiceProviderResponse updateProvider(UUID providerId, UpdateServiceProviderRequest request) {
        log.info("Updating service provider: {}", providerId);

        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));

        if (request.getName() != null) provider.setName(request.getName());
        if (request.getPhone() != null) provider.setPhone(request.getPhone());
        if (request.getEmail() != null) provider.setEmail(request.getEmail());
        if (request.getAddress() != null) provider.setAddress(request.getAddress());
        if (request.getDescription() != null) provider.setDescription(request.getDescription());
        if (request.getStatus() != null) provider.setStatus(request.getStatus());
        if (request.getLogoUrl() != null) provider.setLogoUrl(request.getLogoUrl());
        if (request.getVerified() != null) provider.setVerified(request.getVerified());
        if (request.getCommissionRate() != null) provider.setCommissionRate(request.getCommissionRate());

        return mapToResponse(serviceProviderRepository.save(provider));
    }

    @Transactional
    public ServiceProviderResponse updateCommissionRate(UUID providerId, UpdateProviderCommissionRequest request, UUID userId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
        BigDecimal oldRate = provider.getCommissionRate();
        BigDecimal newRate = request.getCommissionRate() != null ? request.getCommissionRate() : DEFAULT_COMMISSION_RATE;
        provider.setCommissionRate(newRate);
        ServiceProvider saved = serviceProviderRepository.save(provider);
        auditLogService.logAction("COMMISSION_UPDATE", "ServiceProvider", providerId.toString(), userId,
                oldRate != null ? oldRate.toPlainString() : "default",
                newRate.toPlainString(),
                null, null);
        log.info("Commission updated for provider {}: {} -> {}", providerId, oldRate, newRate);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ServiceProviderResponse getProvider(UUID id) {
        return serviceProviderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ServiceProviderResponse> getProviderByUserId(UUID userId) {
        return serviceProviderRepository.findByUserId(userId)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderResponse> getAllProviders() {
        return serviceProviderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ServiceProviderResponse> getProvidersPage(int page, int size, ProviderStatus status, String type) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        String typeNorm = type != null && !type.isBlank() ? type.trim().toUpperCase() : null;

        Page<ServiceProvider> pg;
        if (status != null && typeNorm != null) {
            pg = serviceProviderRepository.findByStatusAndProviderType(status, typeNorm, pr);
        } else if (status != null) {
            pg = serviceProviderRepository.findByStatus(status, pr);
        } else if (typeNorm != null) {
            pg = serviceProviderRepository.findByProviderType(typeNorm, pr);
        } else {
            pg = serviceProviderRepository.findAll(pr);
        }
        return pg.map(this::mapToResponse);
    }

    @Transactional
    public void deleteProvider(UUID providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
        provider.setStatus(ProviderStatus.INACTIVE);
        serviceProviderRepository.save(provider);
        log.info("Provider soft-deleted (INACTIVE): {}", providerId);
    }

    @Transactional
    public ServiceProviderResponse approveProvider(UUID providerId, UUID approverUserId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Service provider not found"));
        if (provider.getStatus() != ProviderStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only providers in PENDING_APPROVAL can be approved");
        }
        provider.setStatus(ProviderStatus.ACTIVE);
        provider.setVerified(true);
        provider.setApprovedBy(approverUserId);
        provider.setApprovedAt(LocalDateTime.now());
        ServiceProvider saved = serviceProviderRepository.save(provider);

        if (saved.getUserId() != null) {
            userRepository.findById(saved.getUserId()).ifPresent(u -> {
                if (u.getRole() == UserRole.PROVIDER_MANAGER) {
                    u.setStatus(UserStatus.ACTIVE);
                    userRepository.save(u);
                    providerWorkflowEmailService.notifyActivated(saved, u.getEmail());
                }
            });
        }

        auditLogService.logAction("PROVIDER_APPROVE", "ServiceProvider", providerId.toString(), approverUserId,
                "PENDING_APPROVAL", "ACTIVE", null, null);
        return mapToResponse(saved);
    }

    /**
     * Reject a pending submission: provider becomes INACTIVE (DB has no REJECTED enum); manager login stays blocked.
     */
    @Transactional
    public ServiceProviderResponse rejectProvider(UUID providerId, UUID actorUserId, String reason) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Service provider not found"));
        if (provider.getStatus() != ProviderStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Only providers in PENDING_APPROVAL can be rejected");
        }
        provider.setStatus(ProviderStatus.INACTIVE);
        provider.setVerified(false);
        ServiceProvider saved = serviceProviderRepository.save(provider);

        if (saved.getUserId() != null) {
            userRepository.findById(saved.getUserId()).ifPresent(u -> {
                if (u.getRole() == UserRole.PROVIDER_MANAGER) {
                    u.setStatus(UserStatus.INACTIVE);
                    userRepository.save(u);
                    providerWorkflowEmailService.notifyRejected(saved, u.getEmail(), reason);
                }
            });
        }

        String newVal = reason != null && !reason.isBlank() ? "REJECTED: " + reason.trim() : "REJECTED";
        auditLogService.logAction("PROVIDER_REJECT", "ServiceProvider", providerId.toString(), actorUserId,
                "PENDING_APPROVAL", newVal, null, null);
        return mapToResponse(saved);
    }

    @Transactional
    public ServiceProviderResponse suspendProvider(UUID providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
        provider.setStatus(ProviderStatus.SUSPENDED);
        return mapToResponse(serviceProviderRepository.save(provider));
    }

    private ServiceProviderResponse mapToResponse(ServiceProvider provider) {
        return ServiceProviderResponse.builder()
                .id(provider.getId())
                .userId(provider.getUserId())
                .name(RequestLocaleHolder.localized(provider.getName(), provider.getNameAr()))
                .type(provider.getType())
                .registrationNumber(provider.getRegistrationNumber())
                .phone(provider.getPhone())
                .email(provider.getEmail())
                .address(provider.getAddress())
                .description(provider.getDescription())
                .logoUrl(provider.getLogoUrl())
                .rating(provider.getRating())
                .reviewCount(provider.getReviewCount())
                .status(provider.getStatus())
                .verified(provider.isVerified())
                .commissionRate(provider.getCommissionRate())
                .createdAt(provider.getCreatedAt())
                .approvedBy(provider.getApprovedBy())
                .approvedAt(provider.getApprovedAt())
                .build();
    }
}
