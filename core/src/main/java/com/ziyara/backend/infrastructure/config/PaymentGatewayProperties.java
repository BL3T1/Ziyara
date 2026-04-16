package com.ziyara.backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Payment gateway configuration (PAYMENT_METHODS, Phase 2).
 * API key and webhook secret should come from env / Secrets Manager in production.
 */
@Component
@ConfigurationProperties(prefix = "app.payment.gateway")
@Getter
@Setter
public class PaymentGatewayProperties {

    private boolean enabled = true;
    private String provider = "stub";
    private String apiKey = "";
    private String apiBaseUrl = "https://api.stripe.com";
    private String webhookSecret = "whsec_change_me";
    private String webhookSignatureHeader = "X-Webhook-Signature";
}
