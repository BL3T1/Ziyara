package com.ziyara.backend.modules.sys.api;

import com.ziyara.backend.application.dto.response.DashboardBootstrapResponse;
import com.ziyara.backend.application.dto.response.DashboardLiveResponse;

import java.time.LocalDate;

/**
 * Dashboard module API (part of sys).
 * Consumers (WebSocket broadcaster) must depend only on this interface.
 */
public interface DashboardServiceApi {

    DashboardLiveResponse loadLive(LocalDate start, LocalDate end, int activityLimit);

    DashboardBootstrapResponse load(LocalDate start, LocalDate end, int activityLimit);
}
