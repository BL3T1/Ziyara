package com.ziyara.backend.application.service;

import com.nulabinc.zxcvbn.Zxcvbn;
import com.ziyara.backend.infrastructure.config.properties.ZiyaraPasswordPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central password complexity rules for all password mutation paths.
 */
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final ZiyaraPasswordPolicyProperties props;
    /** Thread-safe for {@code measure}; dictionary is loaded once per JVM. */
    private final Zxcvbn zxcvbn = new Zxcvbn();

    public void assertAcceptable(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password is required");
        }
        String p = rawPassword;
        if (p.length() < props.getMinLength()) {
            throw new IllegalArgumentException("Password must be at least " + props.getMinLength() + " characters");
        }
        if (props.isRequireUppercase() && p.chars().noneMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("Password must contain an uppercase letter");
        }
        if (props.isRequireLowercase() && p.chars().noneMatch(Character::isLowerCase)) {
            throw new IllegalArgumentException("Password must contain a lowercase letter");
        }
        if (props.isRequireDigit() && p.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Password must contain a digit");
        }
        if (props.isRequireSpecial() && p.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException("Password must contain a special character");
        }
        int minZxcvbn = Math.max(0, Math.min(4, props.getMinZxcvbnScore()));
        if (minZxcvbn > 0) {
            int score = zxcvbn.measure(p).getScore();
            if (score < minZxcvbn) {
                throw new IllegalArgumentException(
                        "Password is too easy to guess (strength " + score + "/4); use a longer or less common phrase.");
            }
        }
    }
}
