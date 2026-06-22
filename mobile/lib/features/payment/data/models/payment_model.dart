import 'package:equatable/equatable.dart';

class PaymentModel extends Equatable {
  final String id;
  final double amount;
  final String status;
  final String? transactionId;
  final String? receiptUrl;

  const PaymentModel({
    required this.id,
    required this.amount,
    required this.status,
    this.transactionId,
    this.receiptUrl,
  });

  factory PaymentModel.fromJson(Map<String, dynamic> json) {
    return PaymentModel(
      id: json['id'],
      amount: json['amount'].toDouble(),
      status: json['status'],
      transactionId: json['transaction_id'],
      receiptUrl: json['receipt_url'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'amount': amount,
      'status': status,
      'transaction_id': transactionId,
      'receipt_url': receiptUrl,
    };
  }

  @override
  List<Object?> get props => [id, amount, status, transactionId, receiptUrl];
}
