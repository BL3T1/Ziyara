import 'package:url_launcher/url_launcher.dart';
import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class LauncherHelper {
  // فتح واتساب
  static Future<void> openWhatsApp(BuildContext context, String phone) async {
    final Uri url = Uri.parse("whatsapp://send?phone=$phone");
    if (!await launchUrl(url)) {
      _showError(context, 'لا يوجد تطبيق واتساب مثبت');
    }
  }

  // إجراء اتصال هاتفي
  static Future<void> makeCall(BuildContext context, String phone) async {
    final Uri url = Uri.parse("tel:$phone");
    if (!await launchUrl(url)) {
      _showError(context, 'تعذر إجراء الاتصال');
    }
  }

  // فتح رابط ويب (للشروط والأحكام مثلاً)
  static Future<void> openUrl(BuildContext context, String urlString) async {
    final Uri url = Uri.parse(urlString);
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      _showError(context, 'تعذر فتح الرابط');
    }
  }

  static void _showError(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: AppColors.error),
    );
  }
}
