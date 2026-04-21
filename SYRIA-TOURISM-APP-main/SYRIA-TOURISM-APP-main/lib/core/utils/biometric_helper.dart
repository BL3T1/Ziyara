import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart' show kIsWeb; // استيراد للتأكد هل نحن ويب أم لا
import 'package:local_auth/local_auth.dart';
import 'package:local_auth_android/local_auth_android.dart';
import 'package:local_auth_ios/local_auth_ios.dart';

class BiometricHelper {
  static final LocalAuthentication _auth = LocalAuthentication();

  static Future<bool> hasBiometrics() async {
    // إذا كنا على الويب، نعيد false فوراً (لا يوجد بصمة)
    if (kIsWeb) return false;

    try {
      final bool canAuthenticateWithBiometrics = await _auth.canCheckBiometrics;
      final bool canAuthenticate = canAuthenticateWithBiometrics || await _auth.isDeviceSupported();
      return canAuthenticate;
    } on PlatformException {
      return false;
    }
  }

  static Future<bool> authenticate() async {
    // إذا كنا على الويب، نعتبر المصادقة ناجحة فوراً (أو نعيد false حسب رغبتك)
    if (kIsWeb) return true;

    try {
      return await _auth.authenticate(
        localizedReason: 'يرجى المصادقة للدخول إلى التطبيق',
        options: const AuthenticationOptions(
          stickyAuth: true,
          biometricOnly: true,
        ),
        authMessages: const <AuthMessages>[
          AndroidAuthMessages(
            signInTitle: 'تسجيل الدخول بالبصمة',
            cancelButton: 'إلغاء',
          ),
          IOSAuthMessages(
            cancelButton: 'إلغاء',
          ),
        ],
      );
    } on PlatformException {
      return false;
    }
  }
}
