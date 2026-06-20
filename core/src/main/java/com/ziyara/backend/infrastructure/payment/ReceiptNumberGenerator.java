package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.domain.repository.CashCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates cash receipt numbers in the format {@code CR-YYYYMMDD-NNNN}, where
 * {@code NNNN} is drawn from the Postgres sequence {@code pay_cash_receipt_seq}
 * (defined in V58). The sequence is monotonically increasing across days; the
 * date component is for human readability, not uniqueness.
 */
@Component
@RequiredArgsConstructor
public class ReceiptNumberGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CashCollectionRepository cashCollectionRepository;

    public String next() {
        long n = cashCollectionRepository.nextReceiptSequence();
        return "CR-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", n);
    }
}
