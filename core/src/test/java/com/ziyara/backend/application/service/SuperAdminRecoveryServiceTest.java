package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.DeletedItemResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.UserRepository;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminRecoveryServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DSLContext dsl;

    @Mock UserRepository userRepository;

    SuperAdminRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminRecoveryService(dsl, userRepository);
    }

    // ── searchCustomers ───────────────────────────────────────────────────────

    @Test
    void searchCustomers_nullQuery_returnsEmpty() {
        List<com.ziyara.backend.application.dto.UserResponse> result = service.searchCustomers(null, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void searchCustomers_blankQuery_returnsEmpty() {
        List<com.ziyara.backend.application.dto.UserResponse> result = service.searchCustomers("   ", 10);

        assertThat(result).isEmpty();
    }

    // ── searchDeleted ─────────────────────────────────────────────────────────

    @Test
    void searchDeleted_nullQuery_returnsEmpty() {
        List<DeletedItemResponse> result = service.searchDeleted(null, 10, Set.of("USER"));

        assertThat(result).isEmpty();
    }

    @Test
    void searchDeleted_blankQuery_returnsEmpty() {
        List<DeletedItemResponse> result = service.searchDeleted("  ", 10, Set.of("USER"));

        assertThat(result).isEmpty();
    }

    // ── restore ───────────────────────────────────────────────────────────────

    @Test
    void restore_nullId_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.restore("USER", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is required");
    }

    @Test
    void restore_invalidEntityType_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.restore("INVALID_TYPE", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType must be USER, SERVICE, or PROVIDER");
    }

    @Test
    void restore_nullEntityType_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.restore(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType must be USER, SERVICE, or PROVIDER");
    }

    // ── permanentDelete ───────────────────────────────────────────────────────

    @Test
    void permanentDelete_nullId_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.permanentDelete("USER", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id is required");
    }

    @Test
    void permanentDelete_invalidEntityType_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.permanentDelete("WIDGET", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType must be USER, SERVICE, or PROVIDER");
    }

    // ── assertActiveCustomer ──────────────────────────────────────────────────

    @Test
    void assertActiveCustomer_userNotFound_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertActiveCustomer(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void assertActiveCustomer_wrongRole_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setRole(UserRole.STAFF);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assertActiveCustomer(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void assertActiveCustomer_softDeletedCustomer_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setRole(UserRole.CUSTOMER);
        user.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assertActiveCustomer(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void assertActiveCustomer_activeCustomer_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setRole(UserRole.CUSTOMER);
        user.setDeletedAt(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Should not throw
        service.assertActiveCustomer(userId);
    }
}
