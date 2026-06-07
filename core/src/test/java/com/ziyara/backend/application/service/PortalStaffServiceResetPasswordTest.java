package com.ziyara.backend.application.service;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.ProviderStaff;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.modules.subscription.api.SubscriptionServiceApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalStaffServiceResetPasswordTest {

    @Mock ProviderStaffRepository providerStaffRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserRbacAssignmentService userRbacAssignmentService;
    @Mock PasswordPolicyService passwordPolicyService;
    @Mock SubscriptionServiceApi subscriptionService;

    @InjectMocks PortalStaffService portalStaffService;

    private static final UUID PROVIDER_ID = UUID.randomUUID();
    private static final UUID OWNER_ID    = UUID.randomUUID();
    private static final UUID STAFF_ID    = UUID.randomUUID();

    @Test
    void resetPassword_memberNotFound_throwsNotFound() {
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(OWNER_ID);
        when(serviceProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));
        when(providerStaffRepository.findByProviderIdAndUserId(PROVIDER_ID, STAFF_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> portalStaffService.resetStaffPassword(PROVIDER_ID, STAFF_ID, "newpass123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPassword_targetIsOwner_throwsBusiness() {
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(OWNER_ID);
        when(serviceProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));

        assertThatThrownBy(() -> portalStaffService.resetStaffPassword(PROVIDER_ID, OWNER_ID, "newpass123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void resetPassword_happyPath_callsUpdatePasswordWithHash() {
        ServiceProvider provider = new ServiceProvider();
        provider.setUserId(OWNER_ID);
        when(serviceProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));

        ProviderStaff link = new ProviderStaff();
        link.setUserId(STAFF_ID);
        when(providerStaffRepository.findByProviderIdAndUserId(PROVIDER_ID, STAFF_ID))
                .thenReturn(Optional.of(link));

        User user = new User();
        user.setId(STAFF_ID);
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass123")).thenReturn("$hashed$");

        portalStaffService.resetStaffPassword(PROVIDER_ID, STAFF_ID, "newpass123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$hashed$");
    }
}
