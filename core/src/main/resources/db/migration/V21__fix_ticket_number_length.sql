-- V21: Fix ticket_number column length in support tables.
--
-- V0 defined both support_complaints.ticket_number and
-- support_internal_tickets.ticket_number as VARCHAR(20).
-- The application generates ticket numbers in the format:
--   CMP-YYYYMMDDHHMI-XXXX  (21 chars for complaints)
--   TKT-YYYYMMDDHHMI-XXXX  (21 chars for internal tickets)
-- These are 21 characters and fail the VARCHAR(20) constraint.
-- Increasing to VARCHAR(30) gives ample room for the current format
-- and any minor future variations.

ALTER TABLE support_complaints
    ALTER COLUMN ticket_number TYPE VARCHAR(30);

ALTER TABLE support_internal_tickets
    ALTER COLUMN ticket_number TYPE VARCHAR(30);
