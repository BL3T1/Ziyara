import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class PaymentSummaryCard extends StatelessWidget {
  final String title;
  final double price;
  final String imageUrl;

  const PaymentSummaryCard({
    super.key,
    required this.title,
    required this.price,
    required this.imageUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Image.network(imageUrl, width: 80, height: 80, fit: BoxFit.cover, errorBuilder: (c, o, s) => Container(color: Colors.grey, width: 80, height: 80)),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                Text('${price.toInt()} ل.س', style: const TextStyle(color: AppColors.primaryBlue)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
