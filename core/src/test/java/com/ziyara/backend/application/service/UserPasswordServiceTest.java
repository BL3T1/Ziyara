package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPasswordServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PasswordHistoryService passwordHistoryService;
    @Mock PasswordPolicyService passwordPolicyService;

    UserPasswordService service;

    @BeforeEach
    void setUp() {
        service = new UserPasswordService(userRepository, passwordEncoder, passwordHistoryService, passwordPolicyService);
    }

    @Test
    void resetPassword_userNotFound_throwsIllegalArgument() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(userId, "NewPass123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void resetPassword_validUser_encodesAndSavesNewPassword() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setPasswordHash("old-hash");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("new-hash");

        service.resetPassword(userId, "NewPass123!");

        verify(passwordPolicyService).assertAcceptable("NewPass123!");
        verify(passwordHistoryService).assertPasswordNotReused(eq(userId), eq("NewPass123!"), eq(passwordEncoder), eq("old-hash"));
        verify(passwordHistoryService).recordPasswordRotation(userId, "old-hash");
        verify(passwordEncoder).encode("NewPass123!");
        verify(userRepository).save(user);
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.isMustChangePassword()).isTrue();
    }

    @Test
    void resetPassword_policyViolation_throwsBeforeRepositoryAccess() {
        UUID userId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Password too weak"))
                .when(passwordPolicyService).assertAcceptable(anyString());

        assertThatThrownBy(() -> service.resetPassword(userId, "weak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password too weak");

        verifyNoInteractions(userRepository);
    }

    @Test
    void resetPassword_passwordReusedViolation_throwsAfterUserFetched() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setPasswordHash("old-hash");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Password was used before"))
                .when(passwordHistoryService).assertPasswordNotReused(any(), anyString(), any(), anyString());

        assertThatThrownBy(() -> service.resetPassword(userId, "NewPass123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password was used before");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_incrementsTokenVersion() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setPasswordHash("old-hash");
        int initialVersion = user.getTokenVersion();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        service.resetPassword(userId, "NewPass123!");

        assertThat(user.getTokenVersion()).isGreaterThan(initialVersion);
    }
}
