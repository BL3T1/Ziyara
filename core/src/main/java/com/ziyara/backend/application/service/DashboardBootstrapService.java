package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ActivityFeedItemResponse;
import com.ziyara.backend.application.dto.response.CommissionAnalysisResponse;
import com.ziyara.backend.application.dto.response.DashboardBootstrapResponse;
import com.ziyara.backend.application.dto.response.DashboardKpiResponse;
import com.ziyara.backend.application.dto.response.DashboardLiveResponse;
import com.ziyara.backend.application.dto.response.PayoutSummaryResponse;
import com.ziyara.backend.application.dto.response.ServiceHealthResponse;
import com.ziyara.backend.application.query.DashboardQueryHandler;
import com.ziyara.backend.infrastructure.config.DashboardExecutorConfig;
import com.ziyara.backend.modules.sys.api.DashboardServiceApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads all dashboard sections in parallel on the server for {@code GET /dashboard/bootstrap}.
 */
@Service
public class DashboardBootstrapService implements DashboardServiceApi {

    private final DashboardService dashboardService;
    private final DashboardQueryHandler dashboardQueryHandler;
    private final Executor dashboardExecutor;

    public DashboardBootstrapService(
            DashboardService dashboardService,
            DashboardQueryHandler dashboardQueryHandler,
            @Qualifier(DashboardExecutorConfig.DASHBOARD_EXECUTOR) Executor dashboardExecutor) {
        this.dashboardService = dashboardService;
        this.dashboardQueryHandler = dashboardQueryHandler;
        this.dashboardExecutor = dashboardExecutor;
    }

    public DashboardBootstrapResponse load(LocalDate start, LocalDate end, int activityLimit) {
        return loadAsync(start, end, activityLimit).join();
    }

    public CompletableFuture<DashboardBootstrapResponse> loadAsync(LocalDate start, LocalDate end, int activityLimit) {
        int lim = Math.min(Math.max(activityLimit, 1), 50);

        CompletableFuture<DashboardKpiResponse> kpisF = CompletableFuture.supplyAsync(
                () -> dashboardService.getKpis(start, end), dashboardExecutor);
        CompletableFuture<List<ActivityFeedItemResponse>> activityF = CompletableFuture.supplyAsync(
                () -> dashboardService.getActivityFeed(lim), dashboardExecutor);
        CompletableFuture<ServiceHealthResponse> healthF = CompletableFuture.supplyAsync(
                dashboardQueryHandler::getServiceHealth, dashboardExecutor);
        CompletableFuture<CommissionAnalysisResponse> commissionF = CompletableFuture.supplyAsync(
                () -> dashboardQueryHandler.getCommissionAnalysis(start, end), dashboardExecutor);
        CompletableFuture<PayoutSummaryResponse> payoutsF = CompletableFuture.supplyAsync(
                () -> dashboardQueryHandler.getPayouts(start, end), dashboardExecutor);

        return CompletableFuture.allOf(kpisF, activityF, healthF, commissionF, payoutsF)
                .thenApply(v -> DashboardBootstrapResponse.builder()
                        .kpis(kpisF.join())
                        .activity(activityF.join())
                        .serviceHealth(healthF.join())
                        .commissionAnalysis(commissionF.join())
                        .payouts(payoutsF.join())
                        .build());
    }

    public DashboardLiveResponse loadLive(LocalDate start, LocalDate end, int activityLimit) {
        return loadLiveAsync(start, end, activityLimit).join();
    }

    public CompletableFuture<DashboardLiveResponse> loadLiveAsync(LocalDate start, LocalDate end, int activityLimit) {
        int lim = Math.min(Math.max(activityLimit, 1), 50);

        CompletableFuture<DashboardKpiResponse> kpisF = CompletableFuture.supplyAsync(
                () -> dashboardService.getKpis(start, end), dashboardExecutor);
        CompletableFuture<List<ActivityFeedItemResponse>> activityF = CompletableFuture.supplyAsync(
                () -> dashboardService.getActivityFeed(lim), dashboardExecutor);
        CompletableFuture<ServiceHealthResponse> healthF = CompletableFuture.supplyAsync(
                dashboardQueryHandler::getServiceHealth, dashboardExecutor);

        return CompletableFuture.allOf(kpisF, activityF, healthF)
                .thenApply(v -> DashboardLiveResponse.builder()
                        .kpis(kpisF.join())
                        .activity(activityF.join())
                        .serviceHealth(healthF.join())
                        .build());
    }
}
