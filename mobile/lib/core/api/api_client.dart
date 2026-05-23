import 'package:dio/dio.dart';

class ApiClient {
  static const String defaultBaseUrl = 'http://localhost:8080/api/v1'; // رابط السيرفر المحلي للباكند
  final Dio _dio;

  ApiClient({String? baseUrl})
      : _dio = Dio(
          BaseOptions(
            baseUrl: baseUrl ?? defaultBaseUrl,
            connectTimeout: const Duration(seconds: 20),
            receiveTimeout: const Duration(seconds: 20),
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
          ),
        ) {
    // إضافة Interceptors للتعامل مع التوكن والأخطاء
    _dio.interceptors.add(LogInterceptor(
      request: true,
      requestBody: true,
      responseBody: true,
      error: true,
    ));
    
    // هنا يمكن إضافة interceptor خاص لإضافة التوكن في الهيدر تلقائياً
  }

  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) async {
    return await _dio.get(path, queryParameters: queryParameters);
  }

  Future<Response> post(String path, {dynamic data}) async {
    return await _dio.post(path, data: data);
  }

  // دوال Put و Delete يمكن إضافتها هنا
}
