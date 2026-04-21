import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/tours_repository.dart';
import 'tour_event.dart';
import 'tour_state.dart';

class TourBloc extends Bloc<TourEvent, TourState> {
  final ToursRepository repository;

  TourBloc({required this.repository}) : super(TourInitial()) {
    on<FetchTours>(_onFetchTours);
  }

  Future<void> _onFetchTours(FetchTours event, Emitter<TourState> emit) async {
    emit(TourLoading());
    try {
      final tours = await repository.getTours();
      emit(TourLoaded(tours));
    } catch (e) {
      emit(TourError(e.toString()));
    }
  }
}
