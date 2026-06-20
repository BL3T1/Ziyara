package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceProviderRequest;
import com.ziyara.backend.application.dto.request.UpdateProviderCommissionRequest;
import com.ziyara.backend.application.dto.request.UpdateServiceProviderRequest;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.ProviderSubscription;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.usecase.provider.ApproveProviderUseCase;
import com.ziyara.backend.domain.usecase.provider.SuspendProviderUseCase;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.infrastructure.security.SecurityRoleUtils;
import com.ziyara.backend.domain.repository.ProviderSubscriptionRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import org.springframework.data.domain.Page;
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
 * Creators with {@code providers:approve} permission activate providers immediately.
 * Others create them as PENDING_APPROVAL for review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10");

    private static final String PROVIDER_MANAGER_ROLE_CODE = "PROVIDER_MANAGER";

    private final ServiceProviderRepository serviceProviderRepository;
    private final UserRepository userRepository;
    private final ProviderSubscriptionRepository providerSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final ProviderWorkflowEmailService providerWorkflowEmailService;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    private final UserPasswordService userPasswordService;
    private final AuthEmailNotificationService authEmailNotificationService;
    private final UserRbacAssignmentService userRbacAssignmentService;

    /**
     * Creates a provider; activation vs pending depends on whether the creator
     * holds {@code providers:approve} (immediate activation) or not (pending review).
     */
    @Transactional
    public ServiceProviderResponse createProvider(CreateServiceProviderRequest request, UUID actorUserId) {
        boolean canApprove = SecurityRoleUtils.canApproveProviders();

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
            managerUser.setRole(UserRole.STAFF);
            managerUser.setStatus(canApprove ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION);
            managerUser = userRepository.save(managerUser);
        }

        if (!createNewManager) {
            managerUser.setStatus(canApprove ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION);
            userRepository.save(managerUser);
        }

        // Assign portal role so the manager has portal:access permissions.
        String roleCode = resolveManagerRoleCode(request.getManagerRole());
        userRbacAssignmentService.assignPrimaryRoleByCode(managerUser.getId(), roleCode);

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
        provider.setRating(BigDecimal.ZERO);
        provider.setReviewCount(0);
        provider.setGlobalRate(request.getGlobalRate());
        provider.setExpiryDate(request.getExpiryDate());

        if (!canApprove) {
            provider.setStatus(ProviderStatus.PENDING_APPROVAL);
            provider.setVerified(false);
        } else {
            provider.setStatus(ProviderStatus.ACTIVE);
            provider.setVerified(true);
            provider.setApprovedBy(actorUserId);
            provider.setApprovedAt(LocalDateTime.now());
        }

        ServiceProvider saved = serviceProviderRepository.save(provider);
        boolean pending = !canApprove;
        String action = pending ? "PROVIDER_SUBMIT_PENDING" : "PROVIDER_CREATE_ACTIVE";
        auditLogService.logAction(action, "ServiceProvider", saved.getId().toString(), actorUserId,
                null, "name=" + saved.getName() + ";userId=" + managerUser.getId(), null, null);
        if (pending) {
            providerWorkflowEmailService.notifySubmittedForApproval(saved, managerUser.getEmail());
            staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                    .eventId(UUID.randomUUID())
                    .notificationType(NotificationType.PROVIDER_PENDING_REVIEW.name())
                    .title("Provider pending approval")
                    .message("Partner \"" + saved.getName() + "\" is waiting for approval.")
                    .notifyRoles(List.of("SALES_MANAGER", "SALES_REPRESENTATIVE", "SUPPORT_MANAGER", "SUPPORT_AGENT", "CEO"))
                    .metadata("{\"providerId\":\"" + saved.getId() + "\"}")
                    .build());
        } else {
            providerWorkflowEmailService.notifyActivated(saved, managerUser.getEmail());
        }
        log.info("{} provider {}", pending ? "Pending" : "Active", saved.getId());
        ensureSubscription(saved.getId(), request.getSubscriptionPlan(), request.getStaffLimit());
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
        if (request.getProfitMargin() != null) provider.setCommissionRate(request.getProfitMargin());
        if (request.getGlobalRate() != null) provider.setGlobalRate(request.getGlobalRate());
        if (request.getExpiryDate() != null) provider.setExpiryDate(request.getExpiryDate());

        return mapToResponse(serviceProviderRepository.save(provider));
    }

    @Transactional
    public ServiceProviderResponse updateCommissionRate(UUID providerId, UpdateProviderCommissionRequest request, UUID userId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Service provider not found"));
        BigDecimal oldRate = provider.getCommissionRate();
        BigDecimal newRate = request.getProfitMargin() != null ? request.getProfitMargin() : DEFAULT_COMMISSION_RATE;
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
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "createdAt", false);
        String typeNorm = type != null && !type.isBlank() ? type.trim().toUpperCase() : null;

        var result = (status != null && typeNorm != null)
                ? serviceProviderRepository.findByStatusAndProviderType(status, typeNorm, query)
                : (status != null)
                    ? serviceProviderRepository.findByStatus(status, query)
                    : (typeNorm != null)
                        ? serviceProviderRepository.findByProviderType(typeNorm, query)
                        : serviceProviderRepository.findAll(query);
        return PageConverter.toSpringPage(result, query, this::mapToResponse);
    }

    @Transactional
    public void deleteProvider(UUID providerId) {
        serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        serviceProviderRepository.softDelete(providerId);
        log.info("Provider soft-deleted (deleted_at set): {}", providerId);
    }

    @Transactional
    public ServiceProviderResponse approveProvider(UUID providerId, UUID approverUserId) {
        var result = new ApproveProviderUseCase(serviceProviderRepository)
                .execute(new ApproveProviderUseCase.Input(providerId, true, approverUserId));
        if (!result.success()) throw new IllegalArgumentException(result.error());
        ServiceProvider saved = result.provider();

        if (saved.getUserId() != null) {
            userRepository.findById(saved.getUserId()).ifPresent(u -> {
                u.setStatus(UserStatus.ACTIVE);
                userRepository.save(u);
                // Ensure PROVIDER_MANAGER role is assigned (idempotent — covers providers
                // created before the createProvider fix was applied).
                userRbacAssignmentService.assignPrimaryRoleByCode(u.getId(), PROVIDER_MANAGER_ROLE_CODE);
                providerWorkflowEmailService.notifyActivated(saved, u.getEmail());
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
                u.setStatus(UserStatus.INACTIVE);
                userRepository.save(u);
                providerWorkflowEmailService.notifyRejected(saved, u.getEmail(), reason);
            });
        }

        String newVal = reason != null && !reason.isBlank() ? "REJECTED: " + reason.trim() : "REJECTED";
        auditLogService.logAction("PROVIDER_REJECT", "ServiceProvider", providerId.toString(), actorUserId,
                "PENDING_APPROVAL", newVal, null, null);
        return mapToResponse(saved);
    }

    @Transactional
    public ServiceProviderResponse suspendProvider(UUID providerId) {
        var result = new SuspendProviderUseCase(serviceProviderRepository)
                .execute(new SuspendProviderUseCase.Input(providerId, null, null));
        if (!result.success()) throw new IllegalArgumentException(result.error());
        return mapToResponse(result.provider());
    }

    private ServiceProviderResponse mapToResponse(ServiceProvider provider) {
        var sub = providerSubscriptionRepository.findByProviderId(provider.getId());
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
                .profitMargin(provider.getCommissionRate())
                .globalRate(provider.getGlobalRate())
                .createdAt(provider.getCreatedAt())
                .approvedBy(provider.getApprovedBy())
                .approvedAt(provider.getApprovedAt())
                .subscriptionPlan(sub.map(s -> s.getPlan()).orElse("FREE"))
                .staffLimit(sub.map(s -> s.getStaffLimit()).orElse(10))
                .expiryDate(provider.getExpiryDate())
                .expired(provider.isExpired())
                .build();
    }

    @Transactional
    public void resetProviderManagerPassword(UUID providerId, String newPassword, UUID actorUserId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Service provider not found"));
        if (provider.getUserId() == null) {
            throw new IllegalArgumentException("Provider has no linked manager account");
        }
        User managerUser = userRepository.findById(provider.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Provider manager user not found"));
        userPasswordService.resetPassword(managerUser.getId(), newPassword);
        auditLogService.logAction("PROVIDER_PASSWORD_RESET", "ServiceProvider", providerId.toString(), actorUserId,
                null, "manager=" + managerUser.getEmail(), null, null);
        log.info("Password reset for provider manager {} by actor {}", managerUser.getEmail(), actorUserId);
    }

    private static final java.util.Set<String> ALLOWED_MANAGER_ROLES = java.util.Set.of(
            "PROVIDER_MANAGER", "PROVIDER_FINANCE", "PROVIDER_STAFF", "TAXI_OPERATOR");

    private static String resolveManagerRoleCode(String requested) {
        if (requested == null || requested.isBlank()) return PROVIDER_MANAGER_ROLE_CODE;
        String code = requested.trim().toUpperCase();
        if (!ALLOWED_MANAGER_ROLES.contains(code)) {
            throw new IllegalArgumentException("Invalid manager role: " + requested +
                    ". Allowed: " + ALLOWED_MANAGER_ROLES);
        }
        return code;
    }

    private void ensureSubscription(UUID providerId, String requestedPlan, Integer requestedLimit) {
        if (providerSubscriptionRepository.findByProviderId(providerId).isPresent()) return;
        String plan = (requestedPlan != null && requestedPlan.trim().equalsIgnoreCase("PRO")) ? "PRO" : "FREE";
        int limit = "PRO".equals(plan) ? (requestedLimit != null && requestedLimit > 0 ? requestedLimit : 10) : 10;
        ProviderSubscription sub = new ProviderSubscription();
        sub.setProviderId(providerId);
        sub.setPlan(plan);
        sub.setStaffLimit(limit);
        providerSubscriptionRepository.save(sub);
    }
}
