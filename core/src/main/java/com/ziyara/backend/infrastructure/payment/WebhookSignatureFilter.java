package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Verifies webhook signature for POST /pay/webhooks before the controller runs (Phase 2).
 * Reads body once, verifies HMAC; if invalid returns 403; if valid wraps request so controller can read body again.
 */
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private final WebhookSignatureVerifier verifier;
    private final PaymentGatewayProperties gatewayProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.endsWith("/pay/webhooks") && !path.endsWith("/webhooks");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        String signatureHeader = request.getHeader(gatewayProperties.getWebhookSignatureHeader());
        if (signatureHeader == null) {
            signatureHeader = request.getHeader("Stripe-Signature"); // common alternative
        }
        if (!verifier.verify(body, signatureHeader)) {
            log.warn("Webhook signature verification failed for /pay/webhooks");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid webhook signature\"}");
            return;
        }
        // Wrap request so controller can read body
        HttpServletRequest wrapped = new CachedBodyRequestWrapper(request, body);
        filterChain.doFilter(wrapped, response);
    }

    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return stream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    // no-op for cached body
                }

                @Override
                public int read() {
                    return stream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }
}

