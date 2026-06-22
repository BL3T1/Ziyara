import 'dart:io';
import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../models/payment_model.dart';
import '../../domain/repositories/payment_repository.dart';

class PaymentRepositoryImpl implements PaymentRepository {
  final ApiClient apiClient;

  PaymentRepositoryImpl({required this.apiClient});

  @override
  Future<double> applyCoupon(String code, double basePrice) async {
    final response = await apiClient.post('/discount-codes/validate', data: {
      'code': code,
      'basePrice': basePrice,
    });
    // Backend ApiResponse wrapper: data is at response.data['data']
    final data = response.data['data'] as Map<String, dynamic>? ?? response.data as Map<String, dynamic>;
    return (data['discountedPrice'] ?? data['discounted_price'] ?? basePrice).toDouble();
  }

  @override
  Future<PaymentModel> processPayment(double amount, Map<String, dynamic> details, File idImage) async {
    final formData = FormData.fromMap({
      'amount': amount,
      ...details,
      'idImage': await MultipartFile.fromFile(idImage.path),
    });

    final response = await apiClient.post('/payments', data: formData);
    final data = response.data['data'] as Map<String, dynamic>? ?? response.data as Map<String, dynamic>;
    return PaymentModel.fromJson(data);
  }
}
