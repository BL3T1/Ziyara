package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import com.ziyara.backend.infrastructure.payment.WebhookSignatureFilter;
import com.ziyara.backend.infrastructure.payment.WebhookSignatureVerifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers webhook signature filter for /pay/webhooks (Phase 2).
 */
@Configuration
public class WebhookFilterConfig {

    @Bean
    public FilterRegistrationBean<WebhookSignatureFilter> webhookSignatureFilter(
            WebhookSignatureVerifier verifier,
            PaymentGatewayProperties gatewayProperties) {
        FilterRegistrationBean<WebhookSignatureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new WebhookSignatureFilter(verifier, gatewayProperties));
        registration.addUrlPatterns("/pay/webhooks");
        registration.setOrder(1);
        return registration;
    }
}

