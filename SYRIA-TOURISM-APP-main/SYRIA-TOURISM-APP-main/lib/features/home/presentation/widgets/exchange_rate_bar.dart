import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class ExchangeRateBar extends StatelessWidget {
  const ExchangeRateBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.primaryBlue,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          // الأيقونة والعنوان الثابت
          const Icon(Icons.currency_exchange, color: AppColors.gold, size: 20),
          const SizedBox(width: 8),
          const Text(
            'الصرف', // اختصرنا النص قليلاً لتوفير مساحة
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          
          const Spacer(), // يدفع العناصر للأطراف
          
          // السعر (محمي من الخطأ)
          Flexible(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Flexible(
                  child: Text(
                    '1 USD = 14,500', 
                    style: const TextStyle(
                      color: AppColors.gold, 
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                    overflow: TextOverflow.ellipsis, // يضع (...) إذا لم يتسع المكان
                    maxLines: 1,
                  ),
                ),
                const SizedBox(width: 4),
                Icon(Icons.arrow_drop_down, color: Colors.red[300], size: 20),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
