# Database hardening — report vs Ziyara inventory

Maps the weaknesses report to **prefixed tables** used after [migrations/015_table_prefix_phase4.sql](migrations/015_table_prefix_phase4.sql). [database/schema.sql](schema.sql) remains unprefixed for greenfield bootstrap; production-like paths use Flyway under `core`.

| Report area | Report artifact | Ziyara target | Existing state | Action |
|-------------|-----------------|---------------|------------------|--------|
| §1.1 Encryption | Plain PII columns | `customers`, `hotel_service_providers`, `pay_payments.gateway_response` | Plaintext | Add optional cipher columns; app encrypts when `ziyara.pii.encryption-key-base64` set |
| §1.2 Password history | `user_password_history` | `sys_user_password_history` → `sys_users` | None | **Add** table + service enforcement |
| §1.3 MFA | MFA columns + logs | `sys_users` + optional verify logs | [009_auth_tokens_otp.sql](migrations/009_auth_tokens_otp.sql): `sys_otp_verification` for email/phone OTP | **Add** TOTP fields on `sys_users`; reuse OTP tables for delivery OTP only |
| §1.4 Sessions | Harden `sessions` | `sessions` (not renamed in 015) | JWT stateless; table may be unused | **Add** invalidation columns for future server-side sessions; **Add** `sys_users.token_version` + JWT `tv` claim for password-change revocation |
| §1.5 Rate limits | `rate_limit_counters` | — | Prefer gateway/Redis | **Defer** (not in 032) |
| §1.6 RLS | Policies on providers/bookings | `hotel_service_providers`, etc. | None | **Opt-in script** [scripts/rls_pilot_hotel_service_providers.sql](scripts/rls_pilot_hotel_service_providers.sql) |
| §2.1 Audit growth | Partition / archive | `sys_audit_logs` | Indexes in V8 | **Add** `sys_audit_logs_archive` + batch job |
| §2.2 / §3.1 / §6.1 Indexes | FK + composite | `sys_user_roles`, `bkg_bookings`, `support_complaints`, `hotel_reviews` | `idx_sys_employees_manager`, `idx_support_complaints_assigned_agent` exist in unprefixed schema; Flyway targets prefixed | **Add** missing FK-style indexes + partial/composites in 032 |
| §4.1 Consent | `user_consents` | `sys_user_consents` | None | **Add** table + API |
| §4.2 Retention | Policies + job | `sys_data_retention_policies` | None | **Add** table + Spring `@Scheduled` job |
| §4.3 Export | Export requests | `sys_data_export_requests` | None | **Add** table + service stub |
| §4.4 PII registry | `pii_field_registry` | `sys_pii_field_registry` | None | **Add** table + seed rows |
| §7.1 Audit columns | correlation_id, … | `sys_audit_logs` | JPA uses `entity_name`, `old_value`, `new_value` | **Defer** wide audit schema change (archive first) |
| §8 Backup | PITR / WAL | Infrastructure | N/A | **Runbook** [ops/BACKUP_PITR_RUNBOOK.md](ops/BACKUP_PITR_RUNBOOK.md) |
| §5.1 Naming | sys_/hotel_/bkg_ | Mixed prefixes | Active JPA matches 015 | **Keep**; document only |

## OTP overlap

- **Login / verification OTP**: `sys_otp_verification` (009).
- **MFA TOTP**: time-based secret on `sys_users` + `TotpService`; not stored in `sys_otp_verification`.

## sys_* rename note

JPA entities map `sys_users`, `sys_audit_logs`, `bkg_bookings`, etc. Migrations **032+** assume prefixed names (same as Flyway V8/V11).
