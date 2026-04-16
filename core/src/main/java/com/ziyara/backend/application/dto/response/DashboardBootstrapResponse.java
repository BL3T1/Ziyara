package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Single payload for initial dashboard load (parallel assembly on server, one HTTP round-trip).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated dashboard data")
public class DashboardBootstrapResponse {

    private DashboardKpiResponse kpis;
    private List<ActivityFeedItemResponse> activity;
    private ServiceHealthResponse serviceHealth;
    private CommissionAnalysisResponse commissionAnalysis;
    private PayoutSummaryResponse payouts;
}
