class AppConstants {
  // API base URL is set at build time via --dart-define=ZIYARA_API_URL=...
  // It is managed centrally in ApiClient. Do NOT hardcode URLs here.

  // القيم الرقمية للتصميم
  static const double defaultPadding = 16.0;
  static const double defaultRadius = 12.0;

  // إعدادات الوقت
  static const int apiTimeout = 30000; // 30 ثانية
  static const int splashDelay = 3; // 3 ثواني

  // دعم الواتساب (يُحدَّث قبل الإطلاق)
  static const String supportWhatsApp = '+963900000000';
}
