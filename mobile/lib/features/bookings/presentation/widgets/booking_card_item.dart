import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class BookingCardItem extends StatelessWidget {
  final String title;
  final String date;
  final String status;
  final double price;
  final bool canCancel;
  final VoidCallback? onCancel;

  const BookingCardItem({
    super.key,
    required this.title,
    required this.date,
    required this.status,
    required this.price,
    required this.canCancel,
    this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: AppColors.primaryBlue.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                child: const Icon(Icons.confirmation_number, color: AppColors.primaryBlue),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    const SizedBox(height: 4),
                    Text(date, style: const TextStyle(color: AppColors.textGrey, fontSize: 13)),
                  ],
                ),
              ),
              if (canCancel)
                TextButton(
                  onPressed: onCancel,
                  child: const Text('إلغاء', style: TextStyle(color: AppColors.error)),
                ),
            ],
          ),
          if (!canCancel) 
            Align(
              alignment: Alignment.centerLeft,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: Colors.grey.shade200, borderRadius: BorderRadius.circular(4)),
                child: Text(status, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ),
        ],
      ),
    );
  }
}
