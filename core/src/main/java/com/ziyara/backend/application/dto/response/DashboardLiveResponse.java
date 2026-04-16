package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Frequently refreshed dashboard slices (polling) without recomputing commission/payout aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "KPIs, activity, and service health for live refresh")
public class DashboardLiveResponse {

    private DashboardKpiResponse kpis;
    private List<ActivityFeedItemResponse> activity;
    private ServiceHealthResponse serviceHealth;
}
