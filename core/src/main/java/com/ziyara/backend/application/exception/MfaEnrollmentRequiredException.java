package com.ziyara.backend.application.exception;

/**
 * Thrown when a user with a privileged role attempts to log in without having TOTP enrolled.
 * Configure the set of roles via {@code ZIYARA_SECURITY_MFA_REQUIRED_ROLES}.
 */
public class MfaEnrollmentRequiredException extends RuntimeException {

    public MfaEnrollmentRequiredException(String message) {
        super(message);
    }
}
