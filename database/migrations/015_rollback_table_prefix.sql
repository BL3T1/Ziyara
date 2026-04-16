-- ============================================================================
-- Phase 4 ROLLBACK: Rename prefixed tables back to original names.
-- Run only if 015_table_prefix_phase4.sql was applied and rollback is needed.
-- ============================================================================

ALTER TABLE sys_users RENAME TO users;
ALTER TABLE sys_roles RENAME TO roles;
ALTER TABLE sys_permissions RENAME TO permissions;
ALTER TABLE sys_groups RENAME TO groups;
ALTER TABLE sys_departments RENAME TO departments;
ALTER TABLE sys_user_roles RENAME TO user_roles;
ALTER TABLE sys_role_permissions RENAME TO role_permissions;
ALTER TABLE sys_audit_logs RENAME TO audit_logs;
ALTER TABLE sys_password_reset_tokens RENAME TO password_reset_tokens;
ALTER TABLE sys_otp_verification RENAME TO otp_verification;
ALTER TABLE sys_employees RENAME TO employees;
ALTER TABLE sys_notifications RENAME TO notifications;

ALTER TABLE hotel_services RENAME TO services;
ALTER TABLE hotel_service_images RENAME TO service_images;
ALTER TABLE hotel_service_providers RENAME TO service_providers;
ALTER TABLE hotel_reviews RENAME TO reviews;

ALTER TABLE bkg_bookings RENAME TO bookings;
ALTER TABLE bkg_taxi_bookings RENAME TO taxi_bookings;

ALTER TABLE pay_payments RENAME TO payments;
ALTER TABLE pay_refunds RENAME TO refunds;
ALTER TABLE pay_exchange_rates RENAME TO exchange_rates;

ALTER TABLE disc_discount_codes RENAME TO discount_codes;

ALTER TABLE support_complaints RENAME TO complaints;
ALTER TABLE support_complaint_comments RENAME TO complaint_comments;
ALTER TABLE support_internal_tickets RENAME TO internal_tickets;
ALTER TABLE support_ticket_comments RENAME TO ticket_comments;
