import 'dart:io';
import '../../data/models/payment_model.dart';

abstract class PaymentRepository {
  Future<double> applyCoupon(String code, double basePrice);
  Future<PaymentModel> processPayment(double amount, Map<String, dynamic> details, File idImage);
}
