import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

class HotelCardShimmer extends StatelessWidget {
  const HotelCardShimmer({super.key});

  @override
  Widget build(BuildContext context) {
    // الألوان المستخدمة للوميض
    final baseColor = Colors.grey[300]!;
    final highlightColor = Colors.grey[100]!;

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Shimmer.fromColors(
        baseColor: baseColor,
        highlightColor: highlightColor,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // محاكاة الصورة (مربع رمادي كبير)
            Container(
              height: 150,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
              ),
            ),
            
            Padding(
              padding: const EdgeInsets.all(12.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // محاكاة العنوان والتقييم
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // خط العنوان
                      Container(height: 16, width: 150, color: Colors.white),
                      // مربع التقييم
                      Container(height: 20, width: 40, color: Colors.white),
                    ],
                  ),
                  const SizedBox(height: 8),
                  
                  // محاكاة الموقع
                  Container(height: 12, width: 100, color: Colors.white),
                  const SizedBox(height: 12),
                  
                  // محاكاة السعر والزر
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(height: 10, width: 50, color: Colors.white),
                          const SizedBox(height: 4),
                          Container(height: 16, width: 80, color: Colors.white),
                        ],
                      ),
                      // محاكاة زر الحجز
                      Container(
                        height: 36,
                        width: 100,
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(20),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
