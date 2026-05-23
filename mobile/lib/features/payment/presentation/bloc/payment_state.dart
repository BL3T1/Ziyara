import 'package:equatable/equatable.dart';
import '../data/models/payment_model.dart';

abstract class PaymentState extends Equatable {
  final int personCount;
  final double discountAmount;
  final bool isCouponApplied;

  const PaymentState({
    this.personCount = 1,
    this.discountAmount = 0.0,
    this.isCouponApplied = false,
  });

  @override
  List<Object?> get props => [personCount, discountAmount, isCouponApplied];
}

class PaymentInitial extends PaymentState {
  const PaymentInitial({super.personCount, super.discountAmount, super.isCouponApplied});
}

class PaymentUpdate extends PaymentState {
  const PaymentUpdate({required super.personCount, required super.discountAmount, required super.isCouponApplied});
}

class PaymentProcessing extends PaymentState {
  const PaymentProcessing({required super.personCount, required super.discountAmount, required super.isCouponApplied});
}

class PaymentSuccess extends PaymentState {
  final PaymentModel payment;
  const PaymentSuccess({
    required this.payment,
    required super.personCount,
    required super.discountAmount,
    required super.isCouponApplied,
  });

  @override
  List<Object?> get props => [payment, ...super.props];
}

class PaymentError extends PaymentState {
  final String message;
  const PaymentError({
    required this.message,
    required super.personCount,
    required super.discountAmount,
    required super.isCouponApplied,
  });

  @override
  List<Object?> get props => [message, ...super.props];
}
