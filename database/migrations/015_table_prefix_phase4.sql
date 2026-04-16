-- ============================================================================
-- Phase 4: Table prefix migration (MODULAR_MONOLITH_STRUCTURE)
-- Renames tables to domain prefixes. Run after 014. Requires DB backup before run.
-- Rollback: run 015_rollback_table_prefix.sql to rename back.
-- ============================================================================

-- sys_
ALTER TABLE users RENAME TO sys_users;
ALTER TABLE roles RENAME TO sys_roles;
ALTER TABLE permissions RENAME TO sys_permissions;
ALTER TABLE groups RENAME TO sys_groups;
ALTER TABLE departments RENAME TO sys_departments;
ALTER TABLE user_roles RENAME TO sys_user_roles;
ALTER TABLE role_permissions RENAME TO sys_role_permissions;
ALTER TABLE audit_logs RENAME TO sys_audit_logs;
ALTER TABLE password_reset_tokens RENAME TO sys_password_reset_tokens;
ALTER TABLE otp_verification RENAME TO sys_otp_verification;
ALTER TABLE employees RENAME TO sys_employees;
ALTER TABLE notifications RENAME TO sys_notifications;

-- hotel_
ALTER TABLE services RENAME TO hotel_services;
ALTER TABLE service_images RENAME TO hotel_service_images;
ALTER TABLE service_providers RENAME TO hotel_service_providers;
ALTER TABLE reviews RENAME TO hotel_reviews;

-- bkg_
ALTER TABLE bookings RENAME TO bkg_bookings;
ALTER TABLE taxi_bookings RENAME TO bkg_taxi_bookings;

-- pay_
ALTER TABLE payments RENAME TO pay_payments;
ALTER TABLE refunds RENAME TO pay_refunds;
ALTER TABLE exchange_rates RENAME TO pay_exchange_rates;

-- disc_
ALTER TABLE discount_codes RENAME TO disc_discount_codes;

-- support_
ALTER TABLE complaints RENAME TO support_complaints;
ALTER TABLE complaint_comments RENAME TO support_complaint_comments;
ALTER TABLE internal_tickets RENAME TO support_internal_tickets;
ALTER TABLE ticket_comments RENAME TO support_ticket_comments;
