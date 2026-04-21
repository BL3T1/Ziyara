import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

class PdfGenerator {
  static Future<void> generateTicket({
    required String title,
    required String price,
    required String date,
    required String userName,
    required String bookingId,
  }) async {
    // تحميل الخط العربي (كايرو) ليعمل داخل الـ PDF
    // سنستخدم الخط الافتراضي للنظام إذا لم نتمكن من تحميل الخط المخصص
    final font = await PdfGoogleFonts.cairoRegular();
    final fontBold = await PdfGoogleFonts.cairoBold();
    
    // تحميل الشعار
    final logoImage = await imageFromAssetBundle('assets/images/logo.png');

    final pdf = pw.Document();

    pdf.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        theme: pw.ThemeData.withFont(
          base: font,
          bold: fontBold,
        ),
        build: (pw.Context context) {
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.center,
            children: [
              // الهيدر: الشعار واسم التطبيق
              pw.Header(
                level: 0,
                child: pw.Row(
                  mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Text('ZIYARA | زيارة', style: pw.TextStyle(fontSize: 24, fontWeight: pw.FontWeight.bold, color: PdfColors.blue900)),
                    pw.Image(logoImage, width: 50, height: 50),
                  ],
                ),
              ),
              
              pw.SizedBox(height: 40),
              
              // عنوان التذكرة
              pw.Text(
                'تذكرة حجز إلكترونية',
                style: pw.TextStyle(fontSize: 28, fontWeight: pw.FontWeight.bold, color: PdfColors.amber700),
              ),
              pw.SizedBox(height: 10),
              pw.Text('يرجى إبراز هذه التذكرة عند الوصول', style: const pw.TextStyle(fontSize: 14, color: PdfColors.grey)),
              
              pw.SizedBox(height: 40),
              
              // تفاصيل الحجز داخل جدول
              pw.Container(
                decoration: pw.BoxDecoration(
                  border: pw.Border.all(color: PdfColors.grey300),
                  borderRadius: pw.BorderRadius.circular(10),
                ),
                padding: const pw.EdgeInsets.all(20),
                child: pw.Column(
                  children: [
                    _buildRow('رقم الحجز', bookingId, fontBold),
                    pw.Divider(),
                    _buildRow('اسم العميل', userName, fontBold),
                    pw.Divider(),
                    _buildRow('الخدمة المحجوزة', title, fontBold),
                    pw.Divider(),
                    _buildRow('تاريخ الحجز', date, fontBold),
                    pw.Divider(),
                    _buildRow('المبلغ المدفوع', '$price ل.س', fontBold, isPrice: true),
                  ],
                ),
              ),
              
              pw.Spacer(),
              
              // QR Code (وهمي للتجربة)
              pw.BarcodeWidget(
                barcode: pw.Barcode.qrCode(),
                data: bookingId,
                width: 100,
                height: 100,
              ),
              pw.SizedBox(height: 10),
              pw.Text('امسح الرمز للتأكد من الحجز', style: const pw.TextStyle(fontSize: 10, color: PdfColors.grey)),
              
              pw.SizedBox(height: 20),
              pw.Footer(
                leading: pw.Text('2025 © ZIYARA App'),
                trailing: pw.Text('دعم العملاء: +963991155614'),
              ),
            ],
          );
        },
      ),
    );

    // عرض ملف الـ PDF للمستخدم ليقوم بطباعته أو مشاركته
    await Printing.layoutPdf(
      onLayout: (PdfPageFormat format) async => pdf.save(),
      name: 'Booking_$bookingId.pdf',
    );
  }

  static pw.Widget _buildRow(String label, String value, pw.Font fontBold, {bool isPrice = false}) {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(vertical: 8),
      child: pw.Row(
        mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        children: [
          pw.Text(value, style: pw.TextStyle(font: fontBold, fontSize: isPrice ? 18 : 14, color: isPrice ? PdfColors.blue900 : PdfColors.black), textDirection: pw.TextDirection.rtl),
          pw.Text(label, style: const pw.TextStyle(fontSize: 14, color: PdfColors.grey700), textDirection: pw.TextDirection.rtl),
        ],
      ),
    );
  }
}
