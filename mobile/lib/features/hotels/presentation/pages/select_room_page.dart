import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/widgets/selection/visual_selector_grid.dart';

class SelectRoomPage extends StatelessWidget {
  final String hotelName;
  final double price;
  final String imageUrl;

  const SelectRoomPage({
    super.key,
    required this.hotelName,
    required this.price,
    required this.imageUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('اختيار الغرفة'), backgroundColor: AppColors.background),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'اختر الغرفة المناسبة لك',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryBlue),
            ),
            const SizedBox(height: 8),
            const Text('المربعات الحمراء محجوزة مسبقاً', style: TextStyle(color: AppColors.textGrey)),
            const SizedBox(height: 30),
            
            // شبكة الغرف
            VisualSelectorGrid(
              totalItems: 20, // 20 غرفة في الطابق
              bookedItems: const [2, 5, 8, 9, 15], // غرف محجوزة وهمية (حمراء)
              itemLabel: 'غرفة',
              onSelected: (roomNum) {
                // يمكن حفظ الرقم المختار هنا
              },
            ),
            
            const SizedBox(height: 50),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () {
                  // الذهاب للدفع مع بيانات الفندق
                  context.push("/payment", extra: {
                    "title": "$hotelName - غرفة مميزة",
                    "price": price,
                    "imageUrl": imageUrl
                  });
                },
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primaryDark,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
                child: const Text('تأكيد الاختيار والمتابعة للدفع'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
