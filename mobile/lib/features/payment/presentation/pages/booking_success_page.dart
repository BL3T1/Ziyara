import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/pdf_generator.dart'; // استيراد المولد

class BookingSuccessPage extends StatelessWidget {
  const BookingSuccessPage({super.key});

  @override
  Widget build(BuildContext context) {
    // توليد رقم حجز عشوائي للتجربة
    final bookingId = '#RES-${DateTime.now().millisecondsSinceEpoch.toString().substring(8)}';
    
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(30.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.check_circle, color: AppColors.success, size: 100),
              const SizedBox(height: 30),
              const Text(
                'تم الحجز بنجاح!',
                style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: AppColors.primaryBlue),
              ),
              const SizedBox(height: 10),
              Text(
                'رقم الحجز: $bookingId',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.textGrey),
              ),
              const SizedBox(height: 30),
              
              // زر تحميل التذكرة الجديد
              ElevatedButton.icon(
                onPressed: () async {
                  // استدعاء دالة توليد التذكرة
                  await PdfGenerator.generateTicket(
                    title: 'فندق شيراتون دمشق', // مثال، يمكن تمرير الاسم الحقيقي لاحقاً
                    price: '450000',
                    date: DateTime.now().toString().split(' ')[0],
                    userName: 'أحمد السوري',
                    bookingId: bookingId,
                  );
                },
                icon: const Icon(Icons.print),
                label: const Text('تحميل التذكرة (PDF)'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.gold,
                  foregroundColor: Colors.black,
                  padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 30),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
              
              const SizedBox(height: 50),
              
              SizedBox(
                width: double.infinity,
                child: OutlinedButton(
                  onPressed: () {
                    context.go('/home');
                  },
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.primaryDark,
                    side: const BorderSide(color: AppColors.primaryDark),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  child: const Text('العودة للرئيسية'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
