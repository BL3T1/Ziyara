import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/hotels_repository.dart';
import 'hotel_event.dart';
import 'hotel_state.dart';

class HotelBloc extends Bloc<HotelEvent, HotelState> {
  final HotelsRepository repository;

  HotelBloc({required this.repository}) : super(HotelInitial()) {
    on<FetchHotels>(_onFetchHotels);
    on<FilterHotels>(_onFilterHotels);
  }

  Future<void> _onFetchHotels(FetchHotels event, Emitter<HotelState> emit) async {
    emit(HotelLoading());
    try {
      final hotels = await repository.getHotels();
      emit(HotelLoaded(allHotels: hotels, filteredHotels: hotels));
    } catch (e) {
      emit(HotelError(e.toString()));
    }
  }

  void _onFilterHotels(FilterHotels event, Emitter<HotelState> emit) {
    if (state is HotelLoaded) {
      final currentState = state as HotelLoaded;
      final query = event.query.toLowerCase();
      
      final filtered = currentState.allHotels.where((hotel) {
        final matchesCity = event.city == 'الكل' || hotel.location == event.city;
        final matchesSearch = hotel.name.toLowerCase().contains(query) || 
                             hotel.location.toLowerCase().contains(query);
        return matchesCity && matchesSearch;
      }).toList();

      emit(HotelLoaded(
        allHotels: currentState.allHotels,
        filteredHotels: filtered,
        selectedCity: event.city,
      ));
    }
  }
}
