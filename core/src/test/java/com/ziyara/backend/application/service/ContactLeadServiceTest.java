package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PublicContactRequest;
import com.ziyara.backend.domain.entity.ContactLead;
import com.ziyara.backend.domain.repository.ContactLeadRepository;
import com.ziyara.backend.application.exception.RateLimitedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactLeadServiceTest {

    @Mock
    ContactLeadRepository repository;

    @InjectMocks
    ContactLeadService service;

    @Test
    void submit_persistsWhenUnderRateLimit() {
        // Non-null IP → combined email+IP check is used
        when(repository.countByEmailAndIpSince(eq("a@b.com"), eq("203.0.113.1"), any())).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PublicContactRequest req = PublicContactRequest.builder()
                .name("Alice")
                .email("a@b.com")
                .company("Co")
                .message("Hello there this is long enough")
                .build();

        service.submit(req, "203.0.113.1");

        ArgumentCaptor<ContactLead> cap = ArgumentCaptor.forClass(ContactLead.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(cap.getValue().getIpAddress()).isEqualTo("203.0.113.1");
    }

    @Test
    void submit_throwsWhenSameEmailAndIpWithinCooldown() {
        // Non-null IP → combined check returns > 0 → rate limited
        when(repository.countByEmailAndIpSince(eq("a@b.com"), eq("203.0.113.1"), any())).thenReturn(1L);

        PublicContactRequest req = PublicContactRequest.builder()
                .name("Alice")
                .email("a@b.com")
                .message("Hello there this is long enough")
                .build();

        assertThatThrownBy(() -> service.submit(req, "203.0.113.1"))
                .isInstanceOf(RateLimitedException.class)
                .hasMessageContaining("wait");
    }

    @Test
    void submit_fallsBackToEmailOnlyWhenIpIsNull() {
        // Null IP → falls back to email-only check
        when(repository.countByEmailSince(eq("a@b.com"), any())).thenReturn(1L);

        PublicContactRequest req = PublicContactRequest.builder()
                .name("Alice")
                .email("a@b.com")
                .message("Hello there this is long enough")
                .build();

        assertThatThrownBy(() -> service.submit(req, null))
                .isInstanceOf(RateLimitedException.class)
                .hasMessageContaining("wait");
    }
}
