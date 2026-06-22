# GDPR & Data Retention

This document describes personal data processing, retention periods, and erasure procedures for the Ziyara platform backend.

---

## Personal Data Stored

| Category | Table(s) | Fields | Purpose |
|---|---|---|---|
| Account identity | `sys_users` | `email`, `phone`, `password_hash` | Authentication |
| Account activity | `sys_users` | `last_login_at`, `last_login_ip`, `failed_login_attempts` | Security monitoring |
| MFA secrets | `sys_users` | `mfa_secret_cipher`, `mfa_backup_codes_cipher` | TOTP authentication; AES-256 encrypted |
| GDPR consent | `sys_users` | `gdpr_consent_given`, `gdpr_consent_date`, `marketing_opt_in` | Consent tracking |
| Password history | `sys_user_password_history` | `password_hash` | Prevent reuse |
| Booking details | `bookings` | customer name via `user_id`, dates, amounts | Service delivery |
| Payment records | `payments`, `refunds` | amounts, gateway references, `user_id` | Financial compliance |
| Audit trail | `sys_audit_logs` | `actor_id`, `ip_address`, action descriptions | Security and compliance |
| Security events | `sys_security_events` | `ip_address`, user agent, event type | Threat detection |
| Rate limit counters | `sys_rate_limit_counters` | `ip_address` | Abuse prevention |
| Notifications | `sys_notifications` | `user_id`, message content | User communication |
| FCM token | `sys_users.fcm_token` | Device push token | Mobile notifications |

---

## Retention Periods

| Category | Retention | Legal basis |
|---|---|---|
| Active user accounts | For account lifetime | Contract |
| Deleted user accounts (`deleted_at` set) | 90 days after deletion | Operational (dispute resolution) |
| Password history | 12 months rolling | Security policy |
| Audit logs | 7 years | Legal/financial compliance |
| Payment records | 7 years | Financial regulation |
| Security events | 90 days | Security monitoring |
| Rate limit counters | 24 hours | Operational |
| OTP records | 10 minutes (hard TTL on `expires_at`) | Session |
| Password reset tokens | 60 minutes (hard TTL) then deleted on use | Session |
| Session tokens (JWT) | Until expiry (access: 24h, refresh: 7d) | Session |

---

## Data Retention Cron

The automated retention job is controlled by:

```yaml
ziyara:
  data-retention:
    enabled: ${ZIYARA_DATA_RETENTION_ENABLED:false}
    cron: ${ZIYARA_DATA_RETENTION_CRON:0 0 4 * * SUN}   # Sundays at 04:00
```

**Enable in production:**
```
ZIYARA_DATA_RETENTION_ENABLED=true
```

The job runs `DataRetentionService` which:
1. Purges `sys_users` records where `deleted_at < NOW() - 90 days`
2. Purges `sys_user_password_history` entries older than 12 months
3. Purges `sys_security_events` older than 90 days
4. Purges expired `sys_rate_limit_counters` entries

**Testing the retention job:**
```bash
# Trigger manually via Spring Actuator (if enabled)
POST /actuator/scheduledtasks/run/dataRetentionJob

# Or run with a short cron interval in dev:
ZIYARA_DATA_RETENTION_CRON="0/30 * * * * *"  # every 30s
```

---

## Right to Erasure (GDPR Article 17)

When a customer requests erasure:

1. Set `sys_users.right_to_erasure_requested = true` (flags the account)
2. The next retention cron run (or manual trigger) will:
   - Soft-delete the user (`deleted_at = NOW()`, `status = DELETED`)
   - Null out PII fields: `email`, `phone`, `mfa_secret_cipher`, `mfa_backup_codes_cipher`, `fcm_token`
   - Delete `sys_user_password_history` entries for the user
   - Anonymize audit log `actor_id` fields referencing the user (set to a tombstone UUID)
3. Set `sys_users.right_to_erasure_completed_at = NOW()`
4. Booking and payment records are **retained** (7-year financial compliance) but unlinked from PII

**Current status:** The `right_to_erasure_requested` flag and `right_to_erasure_completed_at` column exist in `sys_users`. The erasure worker in `DataRetentionService` must be wired to act on `right_to_erasure_requested = true` accounts — verify implementation before going live.

---

## Data Export (Article 20 — Portability)

`sys_data_export_requests` table (V11) tracks export requests. The export worker is not yet implemented. When implementing:
- Export should include: bookings, payments, profile data, notification history
- Format: JSON or CSV
- Delivery: email or signed download URL
- Turnaround: ≤ 30 days per GDPR Article 12(3)

---

## Consent Tracking

Consent is recorded per-user at registration:
- `gdpr_consent_given`: true when the user accepted terms
- `gdpr_consent_date`: timestamp of acceptance
- `marketing_opt_in`: explicit marketing consent (separate from core terms)
- `sys_user_consents` table (V11): full audit trail of consent changes with version and text hash

---

## Data Minimization Notes

- **IP addresses** in `sys_audit_logs` and `sys_security_events` are stored as-is. Consider hashing or truncating (e.g. zeroing the last octet) before the 90-day window if your jurisdiction requires it.
- **Email addresses** in `sys_audit_logs.new_value`/`old_value` JSONB may appear in change records. Audit log anonymization on erasure must cover these fields.
- **JWT tokens** carry `userId` as the `sub` claim. Tokens are invalidated via the blocklist on logout or password change (`token_version` bump).
