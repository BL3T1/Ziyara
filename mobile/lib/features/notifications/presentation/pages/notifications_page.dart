import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/api/api_client.dart';
import '../../../../core/di/injection_container.dart';
import '../../data/notification_model.dart';

class NotificationsPage extends StatefulWidget {
  const NotificationsPage({super.key});

  @override
  State<NotificationsPage> createState() => _NotificationsPageState();
}

class _NotificationsPageState extends State<NotificationsPage> {
  late Future<List<NotificationModel>> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetchNotifications();
  }

  Future<List<NotificationModel>> _fetchNotifications() async {
    try {
      final response = await sl<ApiClient>().get(
        '/notifications',
        queryParameters: {'page': 0, 'size': 30, 'sort': 'createdAt,desc'},
      );
      final raw = response.data['data'];
      final list = raw is Map ? (raw['content'] as List? ?? []) : (raw as List? ?? []);
      return list
          .map((e) => NotificationModel.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> _markAsRead(String id) async {
    try {
      await sl<ApiClient>().put('/notifications/$id/read');
      setState(() => _future = _fetchNotifications());
    } catch (_) {}
  }

  String _formatTime(DateTime dt) {
    final diff = DateTime.now().difference(dt);
    if (diff.inMinutes < 60) return 'منذ ${diff.inMinutes} دقيقة';
    if (diff.inHours < 24) return 'منذ ${diff.inHours} ساعة';
    return 'منذ ${diff.inDays} يوم';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('الإشعارات'),
        backgroundColor: AppColors.background,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => setState(() => _future = _fetchNotifications()),
          ),
        ],
      ),
      body: FutureBuilder<List<NotificationModel>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          final notifications = snapshot.data ?? [];

          if (notifications.isEmpty) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.notifications_off_outlined, size: 64, color: AppColors.textGrey),
                  SizedBox(height: 16),
                  Text('لا توجد إشعارات حالياً', style: TextStyle(color: AppColors.textGrey, fontSize: 16)),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: notifications.length,
            itemBuilder: (context, index) {
              final n = notifications[index];
              return _buildNotificationItem(n);
            },
          );
        },
      ),
    );
  }

  Widget _buildNotificationItem(NotificationModel n) {
    return GestureDetector(
      onTap: () {
        if (!n.isRead) _markAsRead(n.id);
      },
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: n.isRead ? Colors.white : AppColors.primaryBlue.withOpacity(0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: n.isRead ? Colors.grey.shade200 : AppColors.primaryBlue.withOpacity(0.2),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: _iconColor(n.type).withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(_iconFor(n.type), color: _iconColor(n.type), size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    n.title,
                    style: TextStyle(
                      fontWeight: n.isRead ? FontWeight.normal : FontWeight.bold,
                      fontSize: 15,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    n.body,
                    style: const TextStyle(color: AppColors.textGrey, fontSize: 13),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _formatTime(n.createdAt),
                    style: const TextStyle(color: AppColors.textGrey, fontSize: 11),
                  ),
                ],
              ),
            ),
            if (!n.isRead)
              Container(
                width: 8,
                height: 8,
                margin: const EdgeInsets.only(top: 4),
                decoration: const BoxDecoration(
                  color: AppColors.primaryBlue,
                  shape: BoxShape.circle,
                ),
              ),
          ],
        ),
      ),
    );
  }

  IconData _iconFor(String? type) => switch (type) {
        'BOOKING_CONFIRMED' => Icons.check_circle_outline,
        'BOOKING_CANCELLED' => Icons.cancel_outlined,
        'BOOKING_REJECTED' => Icons.thumb_down_outlined,
        'PROMO' || 'OFFER' => Icons.local_offer_outlined,
        'SYSTEM' => Icons.info_outline,
        _ => Icons.notifications_outlined,
      };

  Color _iconColor(String? type) => switch (type) {
        'BOOKING_CONFIRMED' => AppColors.success,
        'BOOKING_CANCELLED' || 'BOOKING_REJECTED' => AppColors.error,
        'PROMO' || 'OFFER' => AppColors.gold,
        _ => AppColors.primaryBlue,
      };
}
