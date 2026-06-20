import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';

class TourCardShimmer extends StatelessWidget {
  const TourCardShimmer({super.key});

  @override
  Widget build(BuildContext context) {
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
          children: [
            // الصورة الكبيرة
            Container(
              height: 180,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // العنوان
                      Container(height: 18, width: 200, color: Colors.white),
                      // التقييم
                      Container(height: 18, width: 40, color: Colors.white),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Container(height: 14, width: 80, color: Colors.white),
                      const SizedBox(width: 16),
                      Container(height: 14, width: 60, color: Colors.white),
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
