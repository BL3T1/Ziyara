package com.ziyara.backend.application.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.ziyara.backend.application.dto.response.BookingReportResponse;
import com.ziyara.backend.application.dto.response.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates Excel (.xlsx) and PDF exports for revenue and booking reports.
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    public byte[] exportRevenueToExcel(RevenueReportResponse report) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Revenue");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Amount (" + report.getCurrency() + ")");

            int rowNum = 1;
            for (RevenueReportResponse.DayTotal day : report.getByDay()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(day.getDate() != null ? day.getDate().toString() : "");
                row.createCell(1).setCellValue(day.getAmount() != null ? day.getAmount().doubleValue() : 0.0);
            }
            Row total = sheet.createRow(rowNum);
            total.createCell(0).setCellValue("TOTAL");
            total.createCell(1).setCellValue(report.getTotalRevenue() != null ? report.getTotalRevenue().doubleValue() : 0.0);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportBookingsToExcel(BookingReportResponse report) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bookings");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Count");

            int rowNum = 1;
            for (BookingReportResponse.DayCount day : report.getByDay()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(day.getDate() != null ? day.getDate().toString() : "");
                row.createCell(1).setCellValue(day.getCount());
            }
            Row total = sheet.createRow(rowNum);
            total.createCell(0).setCellValue("TOTAL");
            total.createCell(1).setCellValue(report.getTotalBookings());

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportRevenueToPdf(RevenueReportResponse report) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            doc.add(new Paragraph("Revenue Report: " + report.getStart() + " – " + report.getEnd(), titleFont));
            doc.add(new Paragraph("Total Revenue: " + report.getTotalRevenue() + " " + report.getCurrency(), bodyFont));
            doc.add(new Paragraph(" "));

            for (RevenueReportResponse.DayTotal day : report.getByDay()) {
                doc.add(new Paragraph(day.getDate() + "  " + day.getAmount() + " " + report.getCurrency(), bodyFont));
            }

            doc.close();
            return out.toByteArray();
        }
    }

    public byte[] exportBookingsToPdf(BookingReportResponse report) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            doc.add(new Paragraph("Booking Report: " + report.getStart() + " – " + report.getEnd(), titleFont));
            doc.add(new Paragraph("Total Bookings: " + report.getTotalBookings(), bodyFont));
            doc.add(new Paragraph(" "));

            for (BookingReportResponse.DayCount day : report.getByDay()) {
                doc.add(new Paragraph(day.getDate() + "  " + day.getCount() + " bookings", bodyFont));
            }

            doc.close();
            return out.toByteArray();
        }
    }
}
