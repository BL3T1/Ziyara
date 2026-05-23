import 'package:equatable/equatable.dart';
import '../../data/models/transport_model.dart';

abstract class TransportState extends Equatable {
  const TransportState();

  @override
  List<Object?> get props => [];
}

class TransportInitial extends TransportState {}

class TransportLoading extends TransportState {}

class TransportLoaded extends TransportState {
  final List<TransportModel> types;
  final String selectedType;
  final double estimatedPrice;

  const TransportLoaded({
    required this.types,
    required this.selectedType,
    required this.estimatedPrice,
  });

  @override
  List<Object?> get props => [types, selectedType, estimatedPrice];
}

class TransportBookingSuccess extends TransportState {
  final String bookingId;
  const TransportBookingSuccess(this.bookingId);

  @override
  List<Object?> get props => [bookingId];
}

class TransportTrackingUpdate extends TransportState {
  final DriverModel driver;
  const TransportTrackingUpdate(this.driver);

  @override
  List<Object?> get props => [driver];
}

class TransportError extends TransportState {
  final String message;
  const TransportError(this.message);

  @override
  List<Object?> get props => [message];
}
