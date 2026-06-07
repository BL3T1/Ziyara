package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.UserConsentResponse;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.entity.UserConsent;
import com.ziyara.backend.domain.repository.UserConsentRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserConsentServiceTest {

    @Mock UserConsentRepository consentRepository;
    @Mock UserRepository userRepository;

    UserConsentService service;

    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserConsentService(consentRepository, userRepository);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_returnsUserConsents() {
        UserConsent consent = consent(userId, "MARKETING_EMAIL", true);
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of(consent));

        List<UserConsentResponse> result = service.list(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConsentType()).isEqualTo("MARKETING_EMAIL");
    }

    @Test
    void list_emptyResult_returnsEmpty() {
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        assertThat(service.list(userId)).isEmpty();
    }

    // ── recordGrant ───────────────────────────────────────────────────────────

    @Test
    void recordGrant_newConsentType_startsAtVersionOne() {
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        UserConsent saved = consent(userId, "TERMS", true);
        saved.setVersion(1);
        when(consentRepository.save(any())).thenReturn(saved);

        service.recordGrant(userId, "TERMS", null, true, null, null);

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(consentRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
    }

    @Test
    void recordGrant_existingConsentType_incrementsVersion() {
        UserConsent existing = consent(userId, "MARKETING_EMAIL", true);
        existing.setVersion(2);
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of(existing));

        UserConsent saved = consent(userId, "MARKETING_EMAIL", false);
        saved.setVersion(3);
        when(consentRepository.save(any())).thenReturn(saved);

        service.recordGrant(userId, "MARKETING_EMAIL", null, false, null, null);

        ArgumentCaptor<UserConsent> captor = ArgumentCaptor.forClass(UserConsent.class);
        verify(consentRepository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
    }

    @Test
    void recordGrant_dataProcessingConsent_setsGdprFlag() {
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        UserConsent saved = consent(userId, "DATA_PROCESSING", true);
        saved.setVersion(1);
        when(consentRepository.save(any())).thenReturn(saved);

        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        service.recordGrant(userId, "DATA_PROCESSING", null, true, null, null);

        assertThat(user.isGdprConsentGiven()).isTrue();
        assertThat(user.getGdprConsentDate()).isNotNull();
    }

    @Test
    void recordGrant_marketingEmailConsent_updatesOptIn() {
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        UserConsent saved = consent(userId, "MARKETING_EMAIL", false);
        saved.setVersion(1);
        when(consentRepository.save(any())).thenReturn(saved);

        User user = new User();
        user.setMarketingOptIn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        service.recordGrant(userId, "MARKETING_EMAIL", null, false, null, null);

        assertThat(user.isMarketingOptIn()).isFalse();
    }

    // ── withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_noActiveConsent_throwsIllegalArgument() {
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.withdraw(userId, "MARKETING_EMAIL", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MARKETING_EMAIL");
    }

    @Test
    void withdraw_activeConsentExists_setsWithdrawnAt() {
        UserConsent active = consent(userId, "MARKETING_EMAIL", true);
        active.setWithdrawnAt(null);
        when(consentRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of(active));
        when(consentRepository.save(any())).thenReturn(active);

        service.withdraw(userId, "MARKETING_EMAIL", "user request");

        assertThat(active.getWithdrawnAt()).isNotNull();
        verify(consentRepository).save(active);
    }

    private UserConsent consent(UUID userId, String type, boolean granted) {
        UserConsent c = new UserConsent();
        c.setId(UUID.randomUUID());
        c.setUserId(userId);
        c.setConsentType(type);
        c.setGranted(granted);
        c.setVersion(1);
        return c;
    }
}
