package com.ziyara.backend.presentation.exception;

/** Too many requests (e.g. public form rate limit). */
public class RateLimitedException extends RuntimeException {
    public RateLimitedException(String message) {
        super(message);
    }
}
