import 'package:equatable/equatable.dart';

abstract class BookingEvent extends Equatable {
  const BookingEvent();

  @override
  List<Object?> get props => [];
}

class FetchBookings extends BookingEvent {}

class CancelBooking extends BookingEvent {
  final String id;
  const CancelBooking(this.id);

  @override
  List<Object?> get props => [id];
}
