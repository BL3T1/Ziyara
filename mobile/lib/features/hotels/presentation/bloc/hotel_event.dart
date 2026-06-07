import 'package:equatable/equatable.dart';

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
