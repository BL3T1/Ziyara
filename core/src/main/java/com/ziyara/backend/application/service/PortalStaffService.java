package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AddPortalStaffRequest;
import com.ziyara.backend.application.dto.request.CreatePortalStaffUserRequest;
import com.ziyara.backend.application.dto.request.UpdatePortalStaffRequest;
import com.ziyara.backend.application.dto.response.PortalStaffMemberResponse;
import com.ziyara.backend.domain.entity.ProviderStaff;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortalStaffService {

    private final ProviderStaffRepository providerStaffRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRbacAssignmentService userRbacAssignmentService;
    private final PasswordPolicyService passwordPolicyService;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public List<PortalStaffMemberResponse> listStaff(UUID providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        List<ProviderStaff> links = providerStaffRepository.findByProviderId(providerId);
        UUID ownerId = provider.getUserId();
        List<UUID> ids = new ArrayList<>();
        if (ownerId != null) ids.add(ownerId);
        for (ProviderStaff link : links) ids.add(link.getUserId());
        Map<UUID, User> usersById = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<PortalStaffMemberResponse> out = new ArrayList<>();
        if (ownerId != null) {
            User u = usersById.get(ownerId);
            if (u != null) out.add(mapUser(u, true, null, null, u.getCreatedAt()));
        }
        for (ProviderStaff link : links) {
            User u = usersById.get(link.getUserId());
            if (u != null) out.add(mapUser(u, false, link.getId(), link.getTitle(), link.getCreatedAt()));
        }
        return out;
    }

    @Transactional
    public PortalStaffMemberResponse addStaff(UUID providerId, AddPortalStaffRequest request) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getUserId();
        if (ownerId != null && ownerId.equals(request.getUserId())) {
            throw new BusinessException("User is already the primary owner of this provider");
        }
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!isProviderPortalRole(user.getRole())) {
            throw new BusinessException("User must have a provider portal role");
        }
        serviceProviderRepository.findByUserId(request.getUserId()).ifPresent(other -> {
            if (!other.getId().equals(providerId)) {
                throw new BusinessException("User is the owner of another provider");
            }
        });
        providerStaffRepository.findByUserId(request.getUserId()).ifPresent(link -> {
            if (!link.getProviderId().equals(providerId)) {
                throw new BusinessException("User is already linked to another provider");
            }
        });
        if (providerStaffRepository.findByProviderIdAndUserId(providerId, request.getUserId()).isPresent()) {
            throw new BusinessException("User is already on this team");
        }

        // Seat-limit check before linking an existing user
        subscriptionService.assertCanAddUser(providerId);

        ProviderStaff staff = new ProviderStaff();
        staff.setProviderId(providerId);
        staff.setUserId(request.getUserId());
        staff.setTitle(trimToNull(request.getTitle()));
        ProviderStaff saved = providerStaffRepository.save(staff);
        return mapUser(user, false, saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    @Transactional
    public PortalStaffMemberResponse createStaffUser(UUID providerId, UUID actorUserId, UserRole actorRole,
                                                      CreatePortalStaffUserRequest request) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        if (actorRole != UserRole.PROVIDER_MANAGER) {
            throw new BusinessException("Only provider managers can create provider staff users");
        }
        UUID ownerId = provider.getUserId();
        boolean linkedMember = providerStaffRepository.findByProviderIdAndUserId(providerId, actorUserId).isPresent();
        if (!actorUserId.equals(ownerId) && !linkedMember) {
            throw new BusinessException("You are not linked to this provider organization");
        }
        UserRole requestedRole = request.getRole();
        if (!isProviderPortalRole(requestedRole)) {
            throw new BusinessException("Role must be a provider portal role");
        }

        // Seat-limit enforcement: reject if the provider's subscription does not
        // allow an additional user. Throws BusinessException with upgrade guidance.
        subscriptionService.assertCanAddUser(providerId);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("User with email already exists");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("User with phone already exists");
        }
        passwordPolicyService.assertAcceptable(request.getPassword());
        User user = new User();
        user.setEmail(email);
        user.setPhone(trimToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(requestedRole);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(user.getId(), user.getRole());
        ProviderStaff staff = new ProviderStaff();
        staff.setProviderId(providerId);
        staff.setUserId(user.getId());
        staff.setTitle(trimToNull(request.getTitle()));
        ProviderStaff saved = providerStaffRepository.save(staff);
        return mapUser(user, false, saved.getId(), saved.getTitle(), saved.getCreatedAt());
    }

    @Transactional
    public PortalStaffMemberResponse updateStaff(UUID providerId, UUID userId, UpdatePortalStaffRequest request) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getUserId();
        if (ownerId != null && ownerId.equals(userId)) {
            throw new BusinessException("Cannot update the owner via staff link; use profile instead");
        }
        ProviderStaff link = providerStaffRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        if (request.getTitle() != null) link.setTitle(trimToNull(request.getTitle()));
        providerStaffRepository.save(link);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapUser(user, false, link.getId(), link.getTitle(), link.getCreatedAt());
    }

    @Transactional
    public void removeStaff(UUID providerId, UUID userId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
        UUID ownerId = provider.getUserId();
        if (ownerId != null && ownerId.equals(userId)) {
            throw new BusinessException("Cannot remove the primary owner from the provider");
        }
        ProviderStaff link = providerStaffRepository.findByProviderIdAndUserId(providerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        providerStaffRepository.deleteById(link.getId());
    }

    private static PortalStaffMemberResponse mapUser(User u, boolean owner, UUID staffLinkId,
                                                      String title, LocalDateTime createdAt) {
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
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
