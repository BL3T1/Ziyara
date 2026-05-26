package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.security.crypto.PiiCryptoService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails startup in production if PII field encryption is not configured.
 * In dev/test the key is optional (no-op mode); in prod it is mandatory.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class PiiEncryptionStartupGuard {

    private final PiiCryptoService piiCryptoService;

    @PostConstruct
    public void validate() {
        if (!piiCryptoService.isConfigured()) {
            throw new IllegalStateException(
                    "PII field encryption is required in production. " +
                    "Set the ZIYARA_PII_ENCRYPTION_KEY_BASE64 environment variable " +
                    "(generate with: openssl rand -base64 32).");
        }
    }
}
