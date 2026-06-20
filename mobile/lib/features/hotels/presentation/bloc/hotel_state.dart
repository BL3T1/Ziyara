import 'package:equatable/equatable.dart';
import '../../data/models/hotel_model.dart';

abstract class HotelState extends Equatable {
  const HotelState();

  @override
  List<Object?> get props => [];
}

class HotelInitial extends HotelState {}

class HotelLoading extends HotelState {}

class HotelLoaded extends HotelState {
  final List<HotelModel> allHotels;
  final List<HotelModel> filteredHotels;
  final String selectedCity;

  const HotelLoaded({
    required this.allHotels,
    required this.filteredHotels,
    this.selectedCity = 'الكل',
  });

  @override
  List<Object?> get props => [allHotels, filteredHotels, selectedCity];
}

class HotelError extends HotelState {
  final String message;

  const HotelError(this.message);

  @override
  List<Object?> get props => [message];
}
