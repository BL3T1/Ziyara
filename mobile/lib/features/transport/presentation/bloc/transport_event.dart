import 'package:equatable/equatable.dart';

abstract class TransportEvent extends Equatable {
  const TransportEvent();

  @override
  List<Object?> get props => [];
}

class FetchTransportTypes extends TransportEvent {}

class SelectCarType extends TransportEvent {
  final String carType;
  const SelectCarType(this.carType);

  @override
  List<Object?> get props => [carType];
}

class BookTransport extends TransportEvent {
  final String from;
  final String to;
  final String type;
  const BookTransport({required this.from, required this.to, required this.type});

  @override
  List<Object?> get props => [from, to, type];
}

class StartTracking extends TransportEvent {
  final String bookingId;
  const StartTracking(this.bookingId);

  @override
  List<Object?> get props => [bookingId];
}
