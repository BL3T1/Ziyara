import 'package:equatable/equatable.dart';

class BookingModel extends Equatable {
  final String id;
  final String title;
  final String date;
  final String status;
  final double price;
  final bool canCancel;

  const BookingModel({
    required this.id,
    required this.title,
    required this.date,
    required this.status,
    required this.price,
    this.canCancel = false,
  });

  factory BookingModel.fromJson(Map<String, dynamic> json) {
    return BookingModel(
      id: json['id'],
      title: json['title'],
      date: json['date'],
      status: json['status'],
      price: json['price'].toDouble(),
      canCancel: json['can_cancel'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'date': date,
      'status': status,
      'price': price,
      'can_cancel': canCancel,
    };
  }

  @override
  List<Object?> get props => [id, title, date, status, price, canCancel];
}
