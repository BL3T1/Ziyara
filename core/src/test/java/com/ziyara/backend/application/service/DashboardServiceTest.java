package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.InternalTicketRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.config.DashboardExecutorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ServiceProviderRepository serviceProviderRepository;
    @Mock
    private InternalTicketRepository internalTicketRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    /** Runs async KPI work synchronously in tests */
    private final Executor sameThreadExecutor = Runnable::run;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                paymentRepository,
                bookingRepository,
                serviceProviderRepository,
                internalTicketRepository,
                auditLogRepository,
                userRepository,
                sameThreadExecutor
        );
    }

    @Test
    void getActivityFeed_resolvesUsersWithSingleBatchFetch() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        AuditLog l1 = new AuditLog();
        l1.setId(UUID.randomUUID());
        l1.setUserId(u1);
        l1.setEntityName("X");
        AuditLog l2 = new AuditLog();
        l2.setId(UUID.randomUUID());
        l2.setUserId(u2);
        l2.setEntityName("X");
        AuditLog l3 = new AuditLog();
        l3.setId(UUID.randomUUID());
        l3.setUserId(u3);
        l3.setEntityName("X");

        when(auditLogRepository.findRecent(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(l1, l2, l3)));

        User usr1 = new User();
        usr1.setId(u1);
        usr1.setEmail("a@test.com");
        User usr2 = new User();
        usr2.setId(u2);
        usr2.setEmail("b@test.com");
        User usr3 = new User();
        usr3.setId(u3);
        usr3.setEmail("c@test.com");
        when(userRepository.findAllById(any())).thenReturn(List.of(usr1, usr2, usr3));

        var items = dashboardService.getActivityFeed(15);

        assertEquals(3, items.size());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> idCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(userRepository, times(1)).findAllById(idCaptor.capture());
        assertEquals(3, idCaptor.getValue().size());

        verify(userRepository, never()).findById(any());
    }
}
