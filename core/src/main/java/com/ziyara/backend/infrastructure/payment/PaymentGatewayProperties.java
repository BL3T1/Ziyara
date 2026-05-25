package com.ziyara.backend.infrastructure.payment;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Payment gateway configuration (PAYMENT_METHODS, Phase 2).
 * API key and webhook secret should come from env / Secrets Manager in production.
 *
 * <p>PAYMENT_WEBHOOK_SECRET is required when the provider is not "stub".
 * Generate with: {@code openssl rand -hex 32}
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
    private String webhookSecret = "";
    private String webhookSignatureHeader = "X-Webhook-Signature";

    @PostConstruct
    void validate() {
        if (enabled && !"stub".equalsIgnoreCase(provider)
                && (webhookSecret == null || webhookSecret.isBlank())) {
            throw new IllegalStateException(
                    "PAYMENT_WEBHOOK_SECRET must be set when PAYMENT_GATEWAY_PROVIDER is not 'stub'. " +
                    "Generate one with: openssl rand -hex 32");
        }
    }
}
