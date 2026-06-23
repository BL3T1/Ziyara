package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateServiceProviderRequest;
import com.ziyara.backend.application.dto.response.ServiceProviderResponse;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.domain.repository.ProviderSubscriptionRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceProviderServiceTest {

    @Mock
    private ServiceProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProviderWorkflowEmailService providerWorkflowEmailService;

    @Mock
    private UserRbacAssignmentService userRbacAssignmentService;

    @Mock
    private StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Mock
    private ProviderSubscriptionRepository providerSubscriptionRepository;

    @InjectMocks
    private ServiceProviderService providerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProvider_superAdmin_createsActiveProvider() {
        CreateServiceProviderRequest request = CreateServiceProviderRequest.builder()
                .name("Test Provider")
                .type("HOTEL")
                .phone("+1000000000")
                .address("Riyadh")
                .managerEmail("manager@test.com")
                .managerPassword("secret1")
                .build();

        when(providerRepository.existsByName("Test Provider")).thenReturn(false);
        when(userRepository.existsByEmail("manager@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        UUID newUserId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(newUserId);
            return u;
        });

        UUID providerId = UUID.randomUUID();
        when(providerRepository.save(any(ServiceProvider.class))).thenAnswer(inv -> {
            ServiceProvider p = inv.getArgument(0);
            p.setId(providerId);
            return p;
        });

        SecurityContextHolder.setContext(new SecurityContextImpl(
                new TestingAuthenticationToken("super", "creds", "ROLE_SUPER_ADMIN")));
        ServiceProviderResponse response = providerService.createProvider(request, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(providerId, response.getId());
        assertEquals(ProviderStatus.ACTIVE, response.getStatus());
        verify(auditLogService).logAction(eq("PROVIDER_CREATE_ACTIVE"), eq("ServiceProvider"), anyString(),
                any(), isNull(), anyString(), isNull(), isNull());
    }

    @Test
    void createProvider_sales_createsPendingProvider() {
        CreateServiceProviderRequest request = CreateServiceProviderRequest.builder()
                .name("Sales Provider")
                .type("HOTEL")
                .phone("+1000000001")
                .address("Dubai")
                .managerEmail("salesmgr@test.com")
                .managerPassword("secret1")
                .build();

        when(providerRepository.existsByName("Sales Provider")).thenReturn(false);
        when(userRepository.existsByEmail("salesmgr@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(providerRepository.save(any(ServiceProvider.class))).thenAnswer(inv -> {
            ServiceProvider p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ServiceProviderResponse response = providerService.createProvider(request, UUID.randomUUID());

        assertEquals(ProviderStatus.PENDING_APPROVAL, response.getStatus());
        verify(auditLogService).logAction(eq("PROVIDER_SUBMIT_PENDING"), eq("ServiceProvider"), anyString(),
                any(), isNull(), anyString(), isNull(), isNull());
    }
}

