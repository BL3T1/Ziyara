package com.ziyara.backend.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Sets {@link AuditRequestContext} and MDC {@code correlationId} from {@code X-Correlation-Id} / {@code X-Request-Id}.
 */
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cid = header(request, "X-Correlation-Id");
        if (cid == null || cid.isBlank()) {
            cid = header(request, "X-Request-Id");
        }
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        String rid = header(request, "X-Request-Id");
        if (rid == null || rid.isBlank()) {
            rid = cid;
        }
        MDC.put("correlationId", cid);
        AuditRequestContext.set(new AuditRequestContext.Holder(cid, rid, null, null, null, null, null, null));
        try {
            response.setHeader("X-Correlation-Id", cid);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            AuditRequestContext.clear();
        }
    }

    private static String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return v == null ? null : v.trim();
    }
}
