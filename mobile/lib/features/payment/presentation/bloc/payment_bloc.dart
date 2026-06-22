import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/payment_repository.dart';
import 'payment_event.dart';
import 'payment_state.dart';

class PaymentBloc extends Bloc<PaymentEvent, PaymentState> {
  final PaymentRepository repository;
  final double basePrice;

  PaymentBloc({required this.repository, required this.basePrice}) : super(const PaymentInitial()) {
    on<UpdatePersonCount>(_onUpdatePersonCount);
    on<ApplyCoupon>(_onApplyCoupon);
    on<ConfirmPayment>(_onConfirmPayment);
  }

  void _onUpdatePersonCount(UpdatePersonCount event, Emitter<PaymentState> emit) {
    emit(PaymentUpdate(
      personCount: event.count,
      discountAmount: state.isCouponApplied ? (basePrice * event.count * 0.10) : 0.0,
      isCouponApplied: state.isCouponApplied,
    ));
  }

  Future<void> _onApplyCoupon(ApplyCoupon event, Emitter<PaymentState> emit) async {
    try {
      final discountedPrice = await repository.applyCoupon(event.code, basePrice * state.personCount);
      final discount = (basePrice * state.personCount) - discountedPrice;
      emit(PaymentUpdate(
        personCount: state.personCount,
        discountAmount: discount,
        isCouponApplied: true,
      ));
    } catch (e) {
      emit(PaymentError(
        message: 'كود الخصم غير صحيح',
        personCount: state.personCount,
        discountAmount: state.discountAmount,
        isCouponApplied: state.isCouponApplied,
      ));
    }
  }

  Future<void> _onConfirmPayment(ConfirmPayment event, Emitter<PaymentState> emit) async {
    emit(PaymentProcessing(
      personCount: state.personCount,
      discountAmount: state.discountAmount,
      isCouponApplied: state.isCouponApplied,
    ));
    try {
      final payment = await repository.processPayment(event.amount, event.details, event.idImage);
      emit(PaymentSuccess(
        payment: payment,
        personCount: state.personCount,
        discountAmount: state.discountAmount,
        isCouponApplied: state.isCouponApplied,
      ));
    } catch (e) {
      emit(PaymentError(
        message: e.toString(),
        personCount: state.personCount,
        discountAmount: state.discountAmount,
        isCouponApplied: state.isCouponApplied,
      ));
    }
  }
}
