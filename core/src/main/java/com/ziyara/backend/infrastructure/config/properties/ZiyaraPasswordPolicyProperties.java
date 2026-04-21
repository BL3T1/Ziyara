package com.ziyara.backend.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ziyara.password-policy")
public class ZiyaraPasswordPolicyProperties {

    private int minLength = 8;

    private boolean requireUppercase = true;

    private boolean requireLowercase = true;

    private boolean requireDigit = true;

    private boolean requireSpecial = true;

    /**
     * Minimum zxcvbn score (0–4). 0 disables the check. When greater than 0, passwords must meet this score after
     * length/charset rules (common passwords and patterns score low).
     */
    private int minZxcvbnScore = 0;
}
