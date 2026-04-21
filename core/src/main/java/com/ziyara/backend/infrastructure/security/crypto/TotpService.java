package com.ziyara.backend.infrastructure.security.crypto;

import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * RFC 6238 TOTP (SHA1, 30s window) compatible with common authenticator apps.
 */
@Service
public class TotpService {

    private static final int DIGITS = 6;
    private static final int WINDOW = 1;
    private final SecureRandom random = new SecureRandom();

    public String generateSecret() {
        byte[] buf = new byte[20];
        random.nextBytes(buf);
        return new Base32().encodeToString(buf).replace("=", "");
    }

    public boolean verify(String base32Secret, String code) {
        if (base32Secret == null || code == null || code.length() != DIGITS) {
            return false;
        }
        String normalized = base32Secret.replace(" ", "").toUpperCase();
        byte[] key = decodeBase32Padded(normalized);
        long counter = Instant.now().getEpochSecond() / 30;
        for (long i = counter - WINDOW; i <= counter + WINDOW; i++) {
            if (code.equals(generateCode(key, i))) {
                return true;
            }
        }
        return false;
    }

    private static String generateCode(byte[] key, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(0, counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String toOtpAuthUri(String issuer, String accountEmail, String secret) {
        String label = issuer + ":" + accountEmail;
        return "otpauth://totp/" + urlEncode(label)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static byte[] decodeBase32Padded(String normalized) {
        StringBuilder sb = new StringBuilder(normalized);
        while (sb.length() % 8 != 0) {
            sb.append('=');
        }
        return new Base32().decode(sb.toString());
    }
}
