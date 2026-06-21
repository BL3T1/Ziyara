-- G5: Retire deprecated CASH_ON_SERVICE / CASH_ON_ARRIVAL values — all map to CASH.
-- Booking table payment_method column
UPDATE bkg_bookings
SET payment_method = 'CASH'
WHERE payment_method IN ('CASH_ON_SERVICE', 'CASH_ON_ARRIVAL');

-- Payments ledger method column
UPDATE pay_payments
SET method = 'CASH'
WHERE method IN ('CASH_ON_SERVICE', 'CASH_ON_ARRIVAL');
