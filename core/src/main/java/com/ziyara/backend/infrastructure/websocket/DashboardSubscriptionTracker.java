package com.ziyara.backend.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class DashboardSubscriptionTracker {

    private static final String DASHBOARD_TOPIC = "/topic/dashboard/live";

    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final AtomicInteger subscriberCount = new AtomicInteger(0);

    public boolean hasSubscribers() {
        return subscriberCount.get() > 0;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (DASHBOARD_TOPIC.equals(destination)) {
            String sessionId = accessor.getSessionId();
            String subId = accessor.getSubscriptionId();
            sessionSubscriptions
                    .computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                    .add(subId);
            int count = subscriberCount.incrementAndGet();
            log.debug("Dashboard subscriber added (session={}, total={})", sessionId, count);
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subId = accessor.getSubscriptionId();
        Set<String> subs = sessionSubscriptions.get(sessionId);
        if (subs != null && subs.remove(subId)) {
            int count = subscriberCount.decrementAndGet();
            log.debug("Dashboard subscriber removed (session={}, total={})", sessionId, count);
            if (subs.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Set<String> subs = sessionSubscriptions.remove(sessionId);
        if (subs != null && !subs.isEmpty()) {
            int removed = subs.size();
            int count = subscriberCount.addAndGet(-removed);
            log.debug("Dashboard session disconnected (session={}, removed={}, total={})",
                    sessionId, removed, count);
        }
    }
}
