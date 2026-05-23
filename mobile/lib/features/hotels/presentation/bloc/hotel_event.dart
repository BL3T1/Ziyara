import 'package:equatable/equatable.dart';
import '../../data/models/hotel_model.dart';

abstract class HotelEvent extends Equatable {
  const HotelEvent();

  @override
  List<Object?> get props => [];
}

class FetchHotels extends HotelEvent {}

class FilterHotels extends HotelEvent {
  final String query;
  final String city;

  const FilterHotels({required this.query, required this.city});

  @override
  List<Object?> get props => [query, city];
}

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
