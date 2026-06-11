package com.ziyara.backend.infrastructure.job;

import com.ziyara.backend.application.dto.response.DashboardLiveResponse;
import com.ziyara.backend.infrastructure.websocket.DashboardSubscriptionTracker;
import com.ziyara.backend.modules.sys.api.DashboardServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardLiveBroadcaster {

    private static final String TOPIC = "/topic/dashboard/live";
    private static final int ACTIVITY_LIMIT = 15;

    private final SimpMessagingTemplate messagingTemplate;
    private final DashboardServiceApi dashboardBootstrapService;
    private final DashboardSubscriptionTracker subscriptionTracker;

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void broadcast() {
        if (!subscriptionTracker.hasSubscribers()) {
            return;
        }
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            DashboardLiveResponse payload = dashboardBootstrapService.loadLive(start, end, ACTIVITY_LIMIT);
            messagingTemplate.convertAndSend(TOPIC, payload);
        } catch (Exception e) {
            log.warn("Dashboard live broadcast failed: {}", e.getMessage());
        }
    }
}
