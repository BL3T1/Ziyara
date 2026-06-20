package com.ziyara.backend.infrastructure.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Optional AES-256-GCM for field-level ciphertext. When no key is configured, encrypt/decrypt are no-ops
 * (plaintext stored — suitable only for local dev).
 */
@Service
public class PiiCryptoService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public PiiCryptoService(@Value("${ziyara.pii.encryption-key-base64:}") String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            this.secretKey = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(keyBase64.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("ziyara.pii.encryption-key-base64 must decode to 32 bytes for AES-256");
        }
        this.secretKey = new SecretKeySpec(raw, AES);
    }

    public boolean isConfigured() {
        return secretKey != null;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (secretKey == null) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("PII encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (secretKey == null) {
            return ciphertext;
        }
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PII decryption failed", e);
        }
    }
}
