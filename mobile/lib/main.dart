import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:shared_preferences/shared_preferences.dart'; // مهم للتحقق
import 'core/theme/app_theme.dart';
import 'core/utils/app_router.dart';
import 'core/utils/app_settings.dart';
import 'core/services/favorites_service.dart';
import 'core/services/search_history_service.dart';

void main() async {
  // ضمان تهيئة النظام
  WidgetsFlutterBinding.ensureInitialized();

  // 1. تحميل البيانات الضرورية أثناء ظهور السبلاش الأصلي (Native Splash)
  await FavoritesService.loadFavorites();
  await SearchHistoryService.loadHistory();

  // 2. التحقق: هل هذه أول مرة يفتح فيها المستخدم التطبيق؟
  final prefs = await SharedPreferences.getInstance();
  final isFirstTime = prefs.getBool('isFirstTime') ?? true;
  
  // تحديد الوجهة: إذا أول مرة -> ترحيب، وإلا -> تسجيل دخول
  final String startRoute = isFirstTime ? '/onboarding' : '/login';

  // إعدادات واجهة النظام
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
    statusBarIconBrightness: Brightness.dark,
  ));

  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);

  runApp(SyriaTourismApp(initialRoute: startRoute));
}

class SyriaTourismApp extends StatelessWidget {
  final String initialRoute;
  const SyriaTourismApp({super.key, required this.initialRoute});

  @override
  Widget build(BuildContext context) {
    // إنشاء الراوتر مع المسار المحدد
    final router = AppRouter.createRouter(initialRoute);

    return ValueListenableBuilder<Locale>(
      valueListenable: AppSettings.localeNotifier,
      builder: (context, locale, child) {
        return ValueListenableBuilder<ThemeMode>(
          valueListenable: AppSettings.themeNotifier,
          builder: (context, themeMode, child) {
            return MaterialApp.router(
              title: 'ZIYARA | زيارة',
              debugShowCheckedModeBanner: false,
              theme: AppTheme.lightTheme,
              darkTheme: AppTheme.darkTheme,
              themeMode: themeMode,
              routerConfig: router,
              locale: locale,
              supportedLocales: const [
                Locale('ar', 'SY'),
                Locale('en', 'US'),
              ],
              localizationsDelegates: const [
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              builder: (context, child) {
                final mediaQueryData = MediaQuery.of(context);
                return MediaQuery(
                  data: mediaQueryData.copyWith(textScaler: const TextScaler.linear(1.0)),
                  child: child!,
                );
              },
            );
          },
        );
      },
    );
  }
}
