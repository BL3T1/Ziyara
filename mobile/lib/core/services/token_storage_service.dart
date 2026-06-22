import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stores JWT tokens in platform-level secure storage:
/// - Android: EncryptedSharedPreferences
/// - iOS: Keychain
class TokenStorageService {
  static const _accessKey = 'ziyara_access_token';
  static const _refreshKey = 'ziyara_refresh_token';

  final FlutterSecureStorage _storage;

  const TokenStorageService({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  Future<void> saveAccessToken(String token) =>
      _storage.write(key: _accessKey, value: token);

  Future<void> saveRefreshToken(String token) =>
      _storage.write(key: _refreshKey, value: token);

  Future<String?> getAccessToken() => _storage.read(key: _accessKey);

  Future<String?> getRefreshToken() => _storage.read(key: _refreshKey);

  Future<void> clearAll() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}
