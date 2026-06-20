import 'package:equatable/equatable.dart';
import '../../data/models/booking_model.dart';

abstract class BookingState extends Equatable {
  const BookingState();

  @override
  List<Object?> get props => [];
}

class BookingInitial extends BookingState {}

class BookingLoading extends BookingState {}

class BookingLoaded extends BookingState {
  final List<BookingModel> activeBookings;
  final List<BookingModel> pastBookings;

  const BookingLoaded({required this.activeBookings, required this.pastBookings});

  @override
  List<Object?> get props => [activeBookings, pastBookings];
}

class BookingError extends BookingState {
  final String message;
  const BookingError(this.message);

  @override
  List<Object?> get props => [message];
}
