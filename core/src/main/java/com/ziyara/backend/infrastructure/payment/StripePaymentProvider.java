package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.payment.GatewayChargeCommand;
import com.ziyara.backend.domain.payment.GatewayChargeResult;
import com.ziyara.backend.domain.payment.GatewayRefundCommand;
import com.ziyara.backend.domain.payment.GatewayRefundResult;
import com.ziyara.backend.domain.payment.PaymentProvider;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Stripe payment gateway implementation. Activated when app.payment.gateway.provider=stripe.
 * Uses Stripe's PaymentIntents API for 3DS-capable card payments.
 * PCI note: raw card data is never handled here â€” only tokens from Stripe.js/Elements.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway.provider", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProvider {

    private static final String STRIPE_API = "https://api.stripe.com/v1";
    private static final String PROVIDER_NAME = "STRIPE";

    private final PaymentGatewayProperties gatewayProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GatewayChargeResult initiatePayment(GatewayChargeCommand command) {
        log.info("Stripe: initiating payment {} amount {} {}", command.paymentId(), command.amount(), command.currency());
        try {
            HttpHeaders headers = buildHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", toStripeAmount(command.amount(), command.currency()));
            body.add("currency", command.currency().toLowerCase());
            body.add("payment_method", command.paymentToken());
            body.add("confirm", "true");
            body.add("automatic_payment_methods[enabled]", "true");
            body.add("automatic_payment_methods[allow_redirects]", "never");
            if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
                headers.set("Idempotency-Key", command.idempotencyKey());
            }
            body.add("metadata[payment_id]", command.paymentId().toString());
            body.add("metadata[booking_id]", command.bookingId().toString());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    STRIPE_API + "/payment_intents", request, Map.class);

            return parsePaymentIntentResponse(command, response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("Stripe API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GatewayChargeResult.failure(command.paymentId(),
                    "Stripe error: " + extractStripeError(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Stripe payment initiation failed", e);
            return GatewayChargeResult.failure(command.paymentId(), "Payment processing unavailable");
        }
    }

    @Override
    public GatewayRefundResult refund(GatewayRefundCommand command) {
        log.info("Stripe: refunding {} {} for payment_intent {}", command.amount(), command.currency(), command.gatewayReference());
        try {
            HttpHeaders headers = buildHeaders();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("payment_intent", command.gatewayReference());
            body.add("amount", toStripeAmount(command.amount(), command.currency()));
            if (command.reason() != null && !command.reason().isBlank()) {
                body.add("reason", "requested_by_customer");
                body.add("metadata[reason]", command.reason().substring(0, Math.min(command.reason().length(), 500)));
            }
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(STRIPE_API + "/refunds", request, Map.class);
            Map<?, ?> resp = response.getBody();
            String refundId = resp != null ? String.valueOf(resp.get("id")) : null;
            return GatewayRefundResult.success(refundId);
        } catch (HttpClientErrorException e) {
            log.error("Stripe refund error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GatewayRefundResult.failure("Stripe refund error: " + extractStripeError(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Stripe refund failed", e);
            return GatewayRefundResult.failure("Refund unavailable");
        }
    }

    @Override
    public Optional<PaymentStatus> getStatus(String gatewayReference) {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    STRIPE_API + "/payment_intents/" + gatewayReference, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) return Optional.empty();
            return Optional.of(mapStripeStatus(String.valueOf(body.get("status"))));
        } catch (Exception e) {
            log.warn("Stripe status check failed for {}: {}", gatewayReference, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(gatewayProperties.getApiKey());
        return headers;
    }

    private static String toStripeAmount(BigDecimal amount, String currency) {
        boolean zeroDecimal = isZeroDecimalCurrency(currency);
        BigDecimal stripe = zeroDecimal
                ? amount.setScale(0, RoundingMode.HALF_UP)
                : amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        return stripe.toPlainString();
    }

    private static boolean isZeroDecimalCurrency(String currency) {
        if (currency == null) return false;
        return switch (currency.toUpperCase()) {
            case "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
                 "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF" -> true;
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private GatewayChargeResult parsePaymentIntentResponse(GatewayChargeCommand command, Map<?, ?> body) {
        if (body == null) {
            return GatewayChargeResult.failure(command.paymentId(), "Empty response from Stripe");
        }
        String status = String.valueOf(body.get("status"));
        String intentId = String.valueOf(body.get("id"));

        if ("requires_action".equals(status)) {
            String redirectUrl = null;
            Map<?, ?> nextAction = (Map<?, ?>) body.get("next_action");
            if (nextAction != null) {
                Map<?, ?> redirect = (Map<?, ?>) nextAction.get("redirect_to_url");
                if (redirect != null) redirectUrl = String.valueOf(redirect.get("url"));
            }
            return GatewayChargeResult.success(command.paymentId(), intentId, intentId, "REQUIRED", redirectUrl);
        }

        PaymentStatus mappedStatus = mapStripeStatus(status);
        if (mappedStatus == PaymentStatus.FAILED) {
            return GatewayChargeResult.failure(command.paymentId(), "Payment declined by Stripe");
        }
        return GatewayChargeResult.success(command.paymentId(), intentId, intentId, "NOT_REQUIRED", null);
    }

    private static PaymentStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "requires_payment_method", "canceled" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private static String extractStripeError(String responseBody) {
        if (responseBody == null) return "Unknown error";
        int msgIdx = responseBody.indexOf("\"message\":");
        if (msgIdx < 0) return responseBody.substring(0, Math.min(200, responseBody.length()));
        int start = responseBody.indexOf('"', msgIdx + 10) + 1;
        int end = responseBody.indexOf('"', start);
        return start > 0 && end > start ? responseBody.substring(start, end)
                : responseBody.substring(0, Math.min(200, responseBody.length()));
    }
}

