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
    final response = await apiClient.post('/payments/coupon/validate', data: {
      'code': code,
      'base_price': basePrice,
    });
    return response.data['discounted_price'].toDouble();
  }

  @override
  Future<PaymentModel> processPayment(double amount, Map<String, dynamic> details, File idImage) async {
    final formData = FormData.fromMap({
      'amount': amount,
      ...details,
      'id_image': await MultipartFile.fromFile(idImage.path),
    });

    final response = await apiClient.post('/payments/process', data: formData);
    return PaymentModel.fromJson(response.data);
  }
}
