import 'package:flutter/material.dart';

class AppSettings {
  // نستخدم ValueNotifier لإبلاغ التطبيق بأي تغيير
  static final ValueNotifier<Locale> localeNotifier = ValueNotifier(const Locale('ar', 'SY'));
  static final ValueNotifier<ThemeMode> themeNotifier = ValueNotifier(ThemeMode.light);

  // دالة تبديل اللغة
  static void toggleLanguage() {
    if (localeNotifier.value.languageCode == 'ar') {
      localeNotifier.value = const Locale('en', 'US');
    } else {
      localeNotifier.value = const Locale('ar', 'SY');
    }
  }

  // دالة تبديل الثيم
  static void toggleTheme() {
    if (themeNotifier.value == ThemeMode.light) {
      themeNotifier.value = ThemeMode.dark;
    } else {
      themeNotifier.value = ThemeMode.light;
    }
  }
  
  // هل اللغة الحالية عربية؟ (مفيد لتغيير النصوص)
  static bool get isArabic => localeNotifier.value.languageCode == 'ar';
}
