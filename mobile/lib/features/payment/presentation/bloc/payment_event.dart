import 'package:equatable/equatable.dart';
import 'dart:io';

abstract class PaymentEvent extends Equatable {
  const PaymentEvent();

  @override
  List<Object?> get props => [];
}

class UpdatePersonCount extends PaymentEvent {
  final int count;
  const UpdatePersonCount(this.count);

  @override
  List<Object?> get props => [count];
}

class ApplyCoupon extends PaymentEvent {
  final String code;
  const ApplyCoupon(this.code);

  @override
  List<Object?> get props => [code];
}

class ConfirmPayment extends PaymentEvent {
  final double amount;
  final Map<String, dynamic> details;
  final File idImage;

  const ConfirmPayment({
    required this.amount,
    required this.details,
    required this.idImage,
  });

  @override
  List<Object?> get props => [amount, details, idImage];
}
