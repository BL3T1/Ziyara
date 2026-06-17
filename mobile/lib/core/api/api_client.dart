import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import '../services/token_storage_service.dart';
import '../error/backend_exception.dart';

/// Central HTTP client.
/// - Base URL is read from the compile-time ZIYARA_API_URL env variable
///   (set in flutter run --dart-define or via a .env + build script).
/// - JWT tokens are attached automatically via [_AuthInterceptor].
/// - On 401, a silent token refresh is attempted once before propagating the error.
class ApiClient {
  static const String _defaultBaseUrl = String.fromEnvironment(
    'ZIYARA_API_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1', // Android emulator localhost alias
  );

  /// The effective base URL used by this client. Exposed for WebSocket URL derivation.
  static String get effectiveBaseUrl => _defaultBaseUrl;

  final Dio _dio;
  final TokenStorageService _tokenStorage;

  ApiClient({String? baseUrl, TokenStorageService? tokenStorage})
      : _tokenStorage = tokenStorage ?? const TokenStorageService(),
        _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl ?? _defaultBaseUrl,
            connectTimeout: const Duration(seconds: 20),
            receiveTimeout: const Duration(seconds: 20),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
          ),
        ) {
    _dio.interceptors.add(_AuthInterceptor(_tokenStorage, _dio));
  }

  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) =>
      _dio.get(path, queryParameters: queryParameters);

  Future<Response> post(String path, {dynamic data}) =>
      _dio.post(path, data: data);

  Future<Response> put(String path, {dynamic data}) =>
      _dio.put(path, data: data);

  Future<Response> delete(String path) => _dio.delete(path);
}

/// Injects Bearer token into every request and silently refreshes on 401.
///
/// Concurrent 401s are queued behind a single in-flight refresh via
/// [_refreshCompleter]. The first request to receive a 401 starts the
/// refresh; all others await the same [Completer] and retry once it resolves.
class _AuthInterceptor extends Interceptor {
  final TokenStorageService _storage;
  final Dio _dio;

  /// Non-null while a token refresh is in progress.
  /// Resolves to the new access token string, or null on failure.
  Completer<String?>? _refreshCompleter;

  _AuthInterceptor(this._storage, this._dio);

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    // Skip auth header for auth endpoints (login / register / refresh)
    if (_isPublicPath(options.path)) {
      handler.next(options);
      return;
    }
    final token = await _storage.getAccessToken();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    // Parse structured backend error before any other handling
    final responseData = err.response?.data;
    if (responseData is Map &&
        responseData['success'] == false &&
        err.response?.statusCode != 401) {
      final code = responseData['code'] as String? ?? 'UNKNOWN_ERROR';
      final message = responseData['message'] as String? ?? '';
      handler.reject(
        DioException(
          requestOptions: err.requestOptions,
          error: BackendException(code: code, rawMessage: message),
          response: err.response,
          type: err.type,
        ),
      );
      return;
    }

    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }

    // Another refresh is already in flight — await its result and retry.
    if (_refreshCompleter != null && !_refreshCompleter!.isCompleted) {
      final newToken = await _refreshCompleter!.future;
      if (newToken != null) {
        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newToken';
        try {
          handler.resolve(await _dio.fetch(retryOptions));
        } catch (e) {
          handler.next(err);
        }
      } else {
        handler.next(err);
      }
      return;
    }

    // This is the first 401 — start the refresh.
    _refreshCompleter = Completer<String?>();
    try {
      final refreshToken = await _storage.getRefreshToken();
      if (refreshToken == null) {
        await _storage.clearAll();
        _refreshCompleter!.complete(null);
        handler.next(err);
        return;
      }

      final refreshResponse = await _dio.post(
        '/auth/refresh',
        data: {'refreshToken': refreshToken},
        options: Options(headers: {'Authorization': null}),
      );
      final newAccess = refreshResponse.data['accessToken'] as String?;
      final newRefresh = refreshResponse.data['refreshToken'] as String?;
      if (newAccess != null) await _storage.saveAccessToken(newAccess);
      if (newRefresh != null) await _storage.saveRefreshToken(newRefresh);

      _refreshCompleter!.complete(newAccess);

      if (newAccess != null) {
        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newAccess';
        try {
          handler.resolve(await _dio.fetch(retryOptions));
        } catch (e) {
          handler.next(err);
        }
      } else {
        await _storage.clearAll();
        handler.next(err);
      }
    } catch (_) {
      await _storage.clearAll();
      _refreshCompleter!.complete(null);
      handler.next(err);
    } finally {
      // Reset after a delay so future 401s (e.g. after token genuinely
      // expires again) can trigger a new refresh cycle.
      Future.delayed(const Duration(seconds: 3), () {
        _refreshCompleter = null;
      });
    }
  }

  static bool _isPublicPath(String path) {
    return path.contains('/auth/login') ||
        path.contains('/auth/register') ||
        path.contains('/auth/refresh') ||
        path.contains('/auth/forgot-password') ||
        path.contains('/auth/reset-password');
  }
}
