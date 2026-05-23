import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class NotificationsPage extends StatelessWidget {
  const NotificationsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('الإشعارات'), backgroundColor: AppColors.background),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildNotificationItem('تم تأكيد حجزك في فندق شيراتون', 'منذ 2 ساعة', true),
          _buildNotificationItem('عرض خاص: خصم 20% على المطاعم', 'منذ يوم', false),
          _buildNotificationItem('يرجى استكمال توثيق الحساب', 'منذ يومين', false),
        ],
      ),
    );
  }

  Widget _buildNotificationItem(String text, String time, bool isUnread) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isUnread ? AppColors.primaryBlue.withOpacity(0.05) : Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: [
          Icon(Icons.notifications, color: isUnread ? AppColors.gold : AppColors.textGrey),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(text, style: TextStyle(fontWeight: isUnread ? FontWeight.bold : FontWeight.normal)),
                const SizedBox(height: 4),
                Text(time, style: const TextStyle(color: AppColors.textGrey, fontSize: 11)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
