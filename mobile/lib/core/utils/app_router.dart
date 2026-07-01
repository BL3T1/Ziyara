import 'package:go_router/go_router.dart';
import 'package:flutter/material.dart';
import '../../features/auth/presentation/pages/login_page.dart';
import '../../features/auth/presentation/pages/signup_page.dart';
import '../../features/auth/presentation/pages/otp_page.dart';
import '../../features/auth/presentation/pages/forgot_password_page.dart';
import '../../features/auth/presentation/pages/mfa_challenge_page.dart';
import '../../features/home/presentation/pages/home_page.dart';
import '../../features/hotels/presentation/pages/hotels_page.dart';
import '../../features/restaurants/presentation/pages/restaurants_page.dart';
import '../../features/tours/presentation/pages/tours_page.dart';
import '../../features/transport/presentation/pages/transport_booking_page.dart';
import '../../features/transport/presentation/pages/driver_tracking_page.dart';
import '../../features/profile/presentation/pages/wallet_page.dart';
import '../../features/profile/presentation/pages/verification_page.dart';
import '../../features/notifications/presentation/pages/notifications_page.dart';
import '../../features/payment/presentation/pages/payment_page.dart';
import '../../features/payment/presentation/pages/booking_success_page.dart';
import '../../features/intro/presentation/pages/onboarding_page.dart';
import '../../features/journey/presentation/pages/journey_page.dart';

class AppRouter {
  // هذه الدالة تنشئ الراوتر مع تحديد صفحة البداية
  static GoRouter createRouter(String initialLocation) {
    return GoRouter(
      initialLocation: initialLocation,
      routes: [
        GoRoute(path: '/onboarding', builder: (context, state) => const OnboardingPage()),
        GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
        GoRoute(path: '/signup', builder: (context, state) => const SignupPage()),
        GoRoute(path: '/otp_verify', builder: (context, state) => const OtpPage()),
        GoRoute(path: '/forgot_password', builder: (context, state) => const ForgotPasswordPage()),
        GoRoute(
          path: '/mfa_challenge',
          builder: (context, state) {
            final extra = state.extra as Map<String, String>;
            return MfaChallengePage(
              email: extra['email']!,
              password: extra['password']!,
            );
          },
        ),
        
        GoRoute(path: '/home', builder: (context, state) => const HomePage()),
        GoRoute(path: '/hotels', builder: (context, state) => const HotelsPage()),
        GoRoute(path: '/restaurants', builder: (context, state) => const RestaurantsPage()),
        GoRoute(path: '/tours', builder: (context, state) => const ToursPage()),
        GoRoute(path: '/transport', builder: (context, state) => const TransportBookingPage()),
        GoRoute(path: '/driver_tracking', builder: (context, state) => const DriverTrackingPage()),
        GoRoute(path: '/wallet', builder: (context, state) => const WalletPage()),
        GoRoute(path: '/verification', builder: (context, state) => const VerificationPage()),
        GoRoute(path: '/notifications', builder: (context, state) => const NotificationsPage()),
        GoRoute(
          path: '/payment',
          builder: (context, state) {
            final extra = state.extra as Map<String, dynamic>;
            return PaymentPage(title: extra['title'], price: extra['price'], imageUrl: extra['imageUrl']);
          },
        ),
        GoRoute(path: '/payment_success', builder: (context, state) => const BookingSuccessPage()),
        GoRoute(path: '/journey', builder: (context, state) => const JourneyPage()),
      ],
    );
  }
}
