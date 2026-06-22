import 'package:get_it/get_it.dart';
import '../api/api_client.dart';
import '../services/token_storage_service.dart';
import '../../features/auth/data/repositories/auth_repository_impl.dart';
import '../../features/auth/domain/repositories/auth_repository.dart';
import '../../features/bookings/data/repositories/booking_repository_impl.dart';
import '../../features/bookings/domain/repositories/booking_repository.dart';
import '../../features/hotels/data/repositories/hotels_repository_impl.dart';
import '../../features/hotels/domain/repositories/hotels_repository.dart';
import '../../features/restaurants/data/repositories/restaurants_repository_impl.dart';
import '../../features/restaurants/domain/repositories/restaurants_repository.dart';
import '../../features/tours/data/repositories/tours_repository_impl.dart';
import '../../features/tours/domain/repositories/tours_repository.dart';
import '../../features/transport/data/repositories/transport_repository_impl.dart';
import '../../features/transport/domain/repositories/transport_repository.dart';
import '../../features/payment/data/repositories/payment_repository_impl.dart';
import '../../features/payment/domain/repositories/payment_repository.dart';
import '../../features/profile/data/repositories/profile_repository_impl.dart';
import '../../features/profile/domain/repositories/profile_repository.dart';
import '../../features/exchange_rate/data/repositories/exchange_rate_repository_impl.dart';

/// Global service locator instance.
final sl = GetIt.instance;

/// Initialises all dependencies. Call once in [main] before [runApp].
Future<void> initDependencies() async {
  // ── Core ────────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<TokenStorageService>(
    () => const TokenStorageService(),
  );

  sl.registerLazySingleton<ApiClient>(
    () => ApiClient(tokenStorage: sl<TokenStorageService>()),
  );

  // ── Auth ────────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(
      apiClient: sl<ApiClient>(),
      tokenStorage: sl<TokenStorageService>(),
    ),
  );

  // ── Bookings ─────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<BookingRepository>(
    () => BookingRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Hotels ───────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<HotelsRepository>(
    () => HotelsRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Restaurants ───────────────────────────────────────────────────────────────
  sl.registerLazySingleton<RestaurantsRepository>(
    () => RestaurantsRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Tours ─────────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<ToursRepository>(
    () => ToursRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Transport ─────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<TransportRepository>(
    () => TransportRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Payment ───────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<PaymentRepository>(
    () => PaymentRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Profile ───────────────────────────────────────────────────────────────────
  sl.registerLazySingleton<ProfileRepository>(
    () => ProfileRepositoryImpl(apiClient: sl<ApiClient>()),
  );

  // ── Exchange Rates ─────────────────────────────────────────────────────────
  sl.registerLazySingleton<ExchangeRateRepositoryImpl>(
    () => ExchangeRateRepositoryImpl(apiClient: sl<ApiClient>()),
  );
}
