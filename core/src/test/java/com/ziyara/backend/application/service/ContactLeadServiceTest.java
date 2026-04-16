package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.PublicContactRequest;
import com.ziyara.backend.infrastructure.persistence.repository.ContactLeadJpaRepository;
import com.ziyara.backend.presentation.exception.RateLimitedException;
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
    ContactLeadJpaRepository repository;

    @InjectMocks
    ContactLeadService service;

    @Test
    void submit_persistsWhenUnderRateLimit() {
        when(repository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("a@b.com"), any())).thenReturn(0L);

        PublicContactRequest req = PublicContactRequest.builder()
                .name("Alice")
                .email("a@b.com")
                .company("Co")
                .message("Hello there this is long enough")
                .build();

        service.submit(req, "203.0.113.1");

        ArgumentCaptor<com.ziyara.backend.infrastructure.persistence.entity.ContactLeadJpaEntity> cap =
                ArgumentCaptor.forClass(com.ziyara.backend.infrastructure.persistence.entity.ContactLeadJpaEntity.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getEmail()).isEqualTo("a@b.com");
        assertThat(cap.getValue().getClientIp()).isEqualTo("203.0.113.1");
    }

    @Test
    void submit_throwsWhenSameEmailWithinCooldown() {
        when(repository.countByEmailIgnoreCaseAndCreatedAtAfter(eq("a@b.com"), any())).thenReturn(1L);

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
