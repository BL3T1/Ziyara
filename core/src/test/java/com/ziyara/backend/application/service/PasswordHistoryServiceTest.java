package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.repository.UserPasswordHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordHistoryServiceTest {

    @Mock UserPasswordHistoryRepository repository;
    @Mock PasswordEncoder encoder;

    PasswordHistoryService service;

    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PasswordHistoryService(repository);
    }

    // ── assertPasswordNotReused ─────��─────────────────────────────────────────

    @Test
    void assertNotReused_matchesCurrentPassword_throws() {
        when(encoder.matches("newPass", "currentHash")).thenReturn(true);

        assertThatThrownBy(() -> service.assertPasswordNotReused(userId, "newPass", encoder, "currentHash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("current password");
    }

    @Test
    void assertNotReused_matchesPreviousPassword_throws() {
        when(encoder.matches("newPass", "currentHash")).thenReturn(false);
        when(repository.findHashesByUserId(userId)).thenReturn(List.of("old1Hash", "old2Hash"));
        when(encoder.matches("newPass", "old1Hash")).thenReturn(false);
        when(encoder.matches("newPass", "old2Hash")).thenReturn(true);

        assertThatThrownBy(() -> service.assertPasswordNotReused(userId, "newPass", encoder, "currentHash"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recent password");
    }

    @Test
    void assertNotReused_uniquePassword_doesNotThrow() {
        when(encoder.matches(any(), any())).thenReturn(false);
        when(repository.findHashesByUserId(userId)).thenReturn(List.of("old1Hash"));

        assertThatNoException().isThrownBy(
                () -> service.assertPasswordNotReused(userId, "newPass", encoder, "currentHash"));
    }

    @Test
    void assertNotReused_noPreviousPasswords_doesNotThrow() {
        when(encoder.matches("newPass", "currentHash")).thenReturn(false);
        when(repository.findHashesByUserId(userId)).thenReturn(List.of());

        assertThatNoException().isThrownBy(
                () -> service.assertPasswordNotReused(userId, "newPass", encoder, "currentHash"));
    }

    // ─�� recordPasswordRotation ──────��─────────────────────────────────────────

    @Test
    void recordPasswordRotation_savesHashAndTrims() {
        List<UUID> ids = IntStream.range(0, 12)
                .mapToObj(i -> UUID.randomUUID())
                .collect(Collectors.toList());
        when(repository.findIdsByUserIdOldestFirst(userId)).thenReturn(ids);

        service.recordPasswordRotation(userId, "oldHash");

        verify(repository).save(userId, "oldHash");
        verify(repository, never()).deleteById(any()); // exactly 12 entries — no trimming needed
    }

    @Test
    void recordPasswordRotation_exceedsMaxHistory_deletesOldest() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ids.add(UUID.randomUUID());
        }
        when(repository.findIdsByUserIdOldestFirst(userId)).thenReturn(ids);

        service.recordPasswordRotation(userId, "oldHash");

        verify(repository).save(userId, "oldHash");
        // 15 entries, max 12 → delete oldest 3
        verify(repository, times(3)).deleteById(any());
    }
}
