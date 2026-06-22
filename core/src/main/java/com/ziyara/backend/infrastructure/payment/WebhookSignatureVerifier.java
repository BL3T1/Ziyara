package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies webhook payload signature using HMAC-SHA256 (PAYMENT_METHODS, Phase 2).
 * Gateway sends signature in header (e.g. X-Webhook-Signature or Stripe-Signature).
 * Algorithm: HMAC-SHA256(secret, rawBody) and compare with header value (hex or base64).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final PaymentGatewayProperties gatewayProperties;

    /**
     * Verify that the payload matches the signature in the given header value.
     *
     * @param rawBody       raw request body (must be exact bytes received)
     * @param signatureHeader value of the signature header (e.g. hex-encoded HMAC)
     * @return true if signature is valid
     */
    public boolean verify(byte[] rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook verification: missing body or signature");
            return false;
        }
        String secret = gatewayProperties.getWebhookSecret();
        if (secret == null || secret.isBlank() || "whsec_change_me".equals(secret)) {
            log.warn("Webhook verification: webhook secret not configured");
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] computed = mac.doFinal(rawBody);
            String computedHex = HexFormat.of().formatHex(computed);
            // Support hex comparison (gateway may send "sha256=hex" or just "hex")
            String expected = signatureHeader.strip();
            if (expected.contains("=")) {
                expected = expected.substring(expected.indexOf('=') + 1).trim();
            }
            return computedHex.equalsIgnoreCase(expected) || constantTimeEquals(computedHex, expected);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification error", e);
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= Character.toLowerCase(a.charAt(i)) - Character.toLowerCase(b.charAt(i));
        }
        return diff == 0;
    }
}

