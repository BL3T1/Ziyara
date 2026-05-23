import '../../../../core/api/api_client.dart';
import '../../../../core/services/push_notification_service.dart';
import '../../../../core/services/token_storage_service.dart';
import '../../domain/repositories/auth_repository.dart';
import '../models/user_model.dart';

class AuthRepositoryImpl implements AuthRepository {
  final ApiClient apiClient;
  final TokenStorageService tokenStorage;

  AuthRepositoryImpl({
    required this.apiClient,
    required this.tokenStorage,
  });

  @override
  Future<UserModel> login(String email, String password, {String? mfaCode}) async {
    final body = <String, dynamic>{'email': email, 'password': password};
    if (mfaCode != null && mfaCode.isNotEmpty) body['mfaCode'] = mfaCode;

    final response = await apiClient.post('/auth/login', data: body);
    final data = response.data as Map<String, dynamic>;

    // Persist tokens to secure storage immediately after successful login
    final accessToken = data['accessToken'] as String?;
    final refreshToken = data['refreshToken'] as String?;
    if (accessToken != null) await tokenStorage.saveAccessToken(accessToken);
    if (refreshToken != null) await tokenStorage.saveRefreshToken(refreshToken);

    // Register FCM push token with the backend (non-blocking, fails silently)
    PushNotificationService.requestPermissionAndGetToken().then((token) {
      if (token != null) PushNotificationService.registerTokenWithBackend(token);
    });

    final userData = data['user'] as Map<String, dynamic>? ?? data;
    return UserModel.fromJson(userData);
  }

  @override
  Future<UserModel> signUp({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  }) async {
    final response = await apiClient.post('/auth/register', data: {
      'email': email,
      'password': password,
      'firstName': firstName,
      'lastName': lastName,
    });
    final data = response.data as Map<String, dynamic>;
    final userData = data['user'] as Map<String, dynamic>? ?? data;
    return UserModel.fromJson(userData);
  }

  @override
  Future<void> logout() async {
    try {
      // Notify backend to blocklist the current token
      await apiClient.post('/auth/logout');
    } catch (_) {
      // Ignore errors — local credential removal must always succeed
    } finally {
      await tokenStorage.clearAll();
    }
  }

  @override
  Future<void> forgotPassword(String email) async {
    await apiClient.post('/auth/forgot-password', data: {'email': email});
  }

  @override
  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    await apiClient.post('/auth/reset-password', data: {
      'token': token,
      'newPassword': newPassword,
    });
  }
}
