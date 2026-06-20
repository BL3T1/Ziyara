package com.ziyara.backend.application.service;

import com.ziyara.backend.infrastructure.config.properties.ZiyaraPasswordPolicyProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyServiceTest {

    @Test
    void zxcvbnDisabledAllowsRepetitivePasswordWhenCharsetRulesMet() {
        ZiyaraPasswordPolicyProperties p = new ZiyaraPasswordPolicyProperties();
        p.setMinZxcvbnScore(0);
        PasswordPolicyService s = new PasswordPolicyService(p);
        assertDoesNotThrow(() -> s.assertAcceptable("aaaaaaaaA1!"));
    }

    @Test
    void zxcvbnMin3RejectsObviousPatterns() {
        ZiyaraPasswordPolicyProperties p = new ZiyaraPasswordPolicyProperties();
        p.setMinZxcvbnScore(3);
        PasswordPolicyService s = new PasswordPolicyService(p);
        assertThrows(IllegalArgumentException.class, () -> s.assertAcceptable("aaaaaaaaA1!"));
    }

    @Test
    void zxcvbnMin3AcceptsStrongPassphrase() {
        ZiyaraPasswordPolicyProperties p = new ZiyaraPasswordPolicyProperties();
        p.setMinZxcvbnScore(3);
        PasswordPolicyService s = new PasswordPolicyService(p);
        assertDoesNotThrow(() -> s.assertAcceptable("correct-horse-battery-staple-7711!X"));
    }
}
