import 'dart:io';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import '../api/api_client.dart';
import '../di/injection_container.dart';

/// Handles FCM push notifications.
///
/// **Setup required before first run:**
/// 1. Create a Firebase project at https://console.firebase.google.com
/// 2. Add an Android app with package name `com.ziyara.app`
/// 3. Download `google-services.json` → place at `android/app/google-services.json`
/// 4. Add to `android/build.gradle` classpath: `com.google.gms:google-services:4.4.2`
/// 5. Add to `android/app/build.gradle` plugin: `com.google.gms.google-services`
///
/// The service degrades gracefully — if Firebase isn't configured the app still runs.
class PushNotificationService {
  static final PushNotificationService _instance =
      PushNotificationService._internal();
  factory PushNotificationService() => _instance;
  PushNotificationService._internal();

  static bool _initialized = false;

  final FlutterLocalNotificationsPlugin _localNotifications =
      FlutterLocalNotificationsPlugin();

  /// Android notification channel — must match the channel declared in AndroidManifest.xml
  static const AndroidNotificationChannel _channel = AndroidNotificationChannel(
    'ziyara_high_importance',
    'Ziyara Notifications',
    description: 'Booking confirmations, offers, and important updates',
    importance: Importance.high,
    playSound: true,
  );

  // ─── Public API ────────────────────────────────────────────────────────────

  /// Initialise Firebase + local notifications.
  /// Safe to call even when google-services.json is absent — returns silently.
  static Future<void> initialize() async {
    if (_initialized) return;
    try {
      await Firebase.initializeApp();
      await _instance._setupLocalNotifications();
      await _instance._setupFcmHandlers();
      _initialized = true;
      debugPrint('[PushNotificationService] Initialized');
    } catch (e) {
      // Firebase not configured (no google-services.json) — degrade gracefully
      debugPrint('[PushNotificationService] Firebase not configured: $e');
    }
  }

  /// Request notification permission (iOS / Android 13+) and return the FCM token.
  /// Returns null if permissions were denied or Firebase isn't configured.
  static Future<String?> requestPermissionAndGetToken() async {
    if (!_initialized) return null;
    try {
      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
        provisional: false,
      );
      if (settings.authorizationStatus == AuthorizationStatus.denied) {
        debugPrint('[PushNotificationService] Permission denied');
        return null;
      }
      final token = await messaging.getToken();
      debugPrint('[PushNotificationService] FCM token: $token');
      return token;
    } catch (e) {
      debugPrint('[PushNotificationService] getToken failed: $e');
      return null;
    }
  }

  /// Register the current device's FCM token with the backend.
  /// Called after successful login so the backend can target this device.
  static Future<void> registerTokenWithBackend(String token) async {
    try {
      await sl<ApiClient>().post(
        '/users/me/fcm-token',
        data: {'token': token},
      );
      debugPrint('[PushNotificationService] Token registered with backend');
    } catch (e) {
      // Non-fatal — token will be re-sent on the next login
      debugPrint('[PushNotificationService] Token registration failed: $e');
    }
  }

  // ─── Private helpers ───────────────────────────────────────────────────────

  Future<void> _setupLocalNotifications() async {
    // Create the high-importance Android channel
    await _localNotifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(_channel);

    const androidInit = AndroidInitializationSettings('@mipmap/launcher_icon');
    const iosInit = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    await _localNotifications.initialize(
      const InitializationSettings(android: androidInit, iOS: iosInit),
      onDidReceiveNotificationResponse: _onNotificationTap,
    );
  }

  Future<void> _setupFcmHandlers() async {
    // Foreground messages — show a local notification because FCM suppresses
    // heads-up banners while the app is in the foreground on Android
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      debugPrint('[PushNotificationService] Foreground message: ${message.messageId}');
      _showLocalNotification(message);
    });

    // Background message tap — app was in background, user tapped notification
    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      debugPrint('[PushNotificationService] Notification opened: ${message.messageId}');
      _handleNotificationNavigation(message.data);
    });

    // Terminated → opened — app was fully closed, user tapped notification
    final initial = await FirebaseMessaging.instance.getInitialMessage();
    if (initial != null) {
      _handleNotificationNavigation(initial.data);
    }

    // Token refresh — re-register whenever FCM rotates the token
    FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
      debugPrint('[PushNotificationService] Token refreshed');
      registerTokenWithBackend(newToken);
    });

    // iOS: show notifications while app is in foreground
    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
      alert: true,
      badge: true,
      sound: true,
    );
  }

  void _showLocalNotification(RemoteMessage message) {
    final notification = message.notification;
    if (notification == null) return;

    _localNotifications.show(
      notification.hashCode,
      notification.title,
      notification.body,
      NotificationDetails(
        android: AndroidNotificationDetails(
          _channel.id,
          _channel.name,
          channelDescription: _channel.description,
          importance: Importance.high,
          priority: Priority.high,
          icon: '@mipmap/launcher_icon',
        ),
        iOS: const DarwinNotificationDetails(
          presentAlert: true,
          presentBadge: true,
          presentSound: true,
        ),
      ),
    );
  }

  void _onNotificationTap(NotificationResponse response) {
    debugPrint('[PushNotificationService] Notification tapped: ${response.payload}');
    // Navigation will be handled per-payload when routing is extended
  }

  void _handleNotificationNavigation(Map<String, dynamic> data) {
    // Future: read data['type'] and data['id'] to deep-link into the app
    debugPrint('[PushNotificationService] Notification data: $data');
  }
}

/// Top-level handler for background FCM messages (required by firebase_messaging).
/// Must be a top-level function (not a class method).
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  debugPrint('[FCM Background] Message: ${message.messageId}');
}
