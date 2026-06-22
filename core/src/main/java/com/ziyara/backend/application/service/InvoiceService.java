package com.ziyara.backend.application.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont  = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(0x0D, 0x1B, 0x2A));
            Font labelFont  = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(0x2D, 0x3A, 0x4B));
            Font valueFont  = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

            Paragraph title = new Paragraph("PAYMENT RECEIPT", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            Paragraph sub = new Paragraph("Ziyara Platform", new Font(Font.HELVETICA, 11, Font.NORMAL, Color.GRAY));
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20f);
            doc.add(sub);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35f, 65f});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);

            addRow(table, "Receipt ID", payment.getId().toString(), labelFont, valueFont);
            addRow(table, "Booking ID",
                    payment.getBookingId() != null ? payment.getBookingId().toString() : "—",
                    labelFont, valueFont);
            addRow(table, "Amount",
                    (payment.getCurrency() != null ? payment.getCurrency() : "USD")
                    + " " + (payment.getAmount() != null ? payment.getAmount().toPlainString() : "0.00"),
                    labelFont, valueFont);
            addRow(table, "Method",
                    payment.getMethod() != null ? payment.getMethod().name() : "—",
                    labelFont, valueFont);
            addRow(table, "Status",
                    payment.getStatus() != null ? payment.getStatus().name() : "—",
                    labelFont, valueFont);
            if (payment.getTransactionReference() != null && !payment.getTransactionReference().isBlank()) {
                addRow(table, "Transaction Ref", payment.getTransactionReference(), labelFont, valueFont);
            }
            if (payment.getGatewayName() != null && !payment.getGatewayName().isBlank()) {
                addRow(table, "Gateway", payment.getGatewayName(), labelFont, valueFont);
            }
            if (payment.getProcessedAt() != null) {
                addRow(table, "Processed At", payment.getProcessedAt().toString(), labelFont, valueFont);
            }
            if (payment.getCreatedAt() != null) {
                addRow(table, "Created At", payment.getCreatedAt().toString(), labelFont, valueFont);
            }

            doc.add(table);

            Paragraph footer = new Paragraph("This is a system-generated receipt. No signature required.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF for payment " + paymentId, e);
        }
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setPadding(8f);
        labelCell.setBackgroundColor(new Color(0xF0, 0xF4, 0xF8));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setPadding(8f);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
