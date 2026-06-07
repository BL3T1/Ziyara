package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ActivityFeedItemResponse;
import com.ziyara.backend.application.dto.response.CommissionAnalysisResponse;
import com.ziyara.backend.application.dto.response.DashboardBootstrapResponse;
import com.ziyara.backend.application.dto.response.DashboardKpiResponse;
import com.ziyara.backend.application.dto.response.DashboardLiveResponse;
import com.ziyara.backend.application.dto.response.PayoutSummaryResponse;
import com.ziyara.backend.application.dto.response.ServiceHealthResponse;
import com.ziyara.backend.application.query.DashboardQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardBootstrapServiceTest {

    @Mock DashboardService dashboardService;
    @Mock DashboardQueryHandler dashboardQueryHandler;

    DashboardBootstrapService service;

    LocalDate start = LocalDate.of(2024, 1, 1);
    LocalDate end = LocalDate.of(2024, 1, 31);

    @BeforeEach
    void setUp() {
        // Run tasks synchronously in tests using a direct (inline) executor
        Executor syncExecutor = Runnable::run;
        service = new DashboardBootstrapService(dashboardService, dashboardQueryHandler, syncExecutor);

        // Default stubs
        when(dashboardService.getKpis(any(), any())).thenReturn(DashboardKpiResponse.builder().build());
        when(dashboardService.getActivityFeed(anyInt())).thenReturn(List.of());
        when(dashboardQueryHandler.getServiceHealth()).thenReturn(ServiceHealthResponse.builder().build());
        when(dashboardQueryHandler.getCommissionAnalysis(any(), any())).thenReturn(CommissionAnalysisResponse.builder().build());
        when(dashboardQueryHandler.getPayouts(any(), any())).thenReturn(PayoutSummaryResponse.builder().build());
    }

    // ── activityLimit clamping ────────────────────────────────────────────────

    @Test
    void load_activityLimitZero_clampedToOne() {
        service.load(start, end, 0);

        verify(dashboardService).getActivityFeed(1);
    }

    @Test
    void load_activityLimitNegative_clampedToOne() {
        service.load(start, end, -5);

        verify(dashboardService).getActivityFeed(1);
    }

    @Test
    void load_activityLimitOverMax_clampedToFifty() {
        service.load(start, end, 200);

        verify(dashboardService).getActivityFeed(50);
    }

    @Test
    void load_activityLimitWithinBounds_passedThrough() {
        service.load(start, end, 25);

        verify(dashboardService).getActivityFeed(25);
    }

    @Test
    void load_activityLimitAtMax_notClamped() {
        service.load(start, end, 50);

        verify(dashboardService).getActivityFeed(50);
    }

    // ── load assembles all sections ───────────────────────────────────────────

    @Test
    void load_returnsFullBootstrapResponse() {
        DashboardBootstrapResponse result = service.load(start, end, 10);

        assertThat(result).isNotNull();
        assertThat(result.getKpis()).isNotNull();
        assertThat(result.getActivity()).isNotNull();
        assertThat(result.getServiceHealth()).isNotNull();
        assertThat(result.getCommissionAnalysis()).isNotNull();
        assertThat(result.getPayouts()).isNotNull();
    }

    // ── loadLive activityLimit clamping ───────────────────────────────────────

    @Test
    void loadLive_activityLimitZero_clampedToOne() {
        service.loadLive(start, end, 0);

        verify(dashboardService).getActivityFeed(1);
    }

    @Test
    void loadLive_activityLimitOverMax_clampedToFifty() {
        service.loadLive(start, end, 999);

        verify(dashboardService).getActivityFeed(50);
    }

    @Test
    void loadLive_returnsLiveResponseWithoutCommissionOrPayouts() {
        DashboardLiveResponse result = service.loadLive(start, end, 10);

        assertThat(result).isNotNull();
        assertThat(result.getKpis()).isNotNull();
        assertThat(result.getActivity()).isNotNull();
        assertThat(result.getServiceHealth()).isNotNull();
        // loadLive does NOT call commission/payouts
        verify(dashboardQueryHandler, never()).getCommissionAnalysis(any(), any());
        verify(dashboardQueryHandler, never()).getPayouts(any(), any());
    }
}
