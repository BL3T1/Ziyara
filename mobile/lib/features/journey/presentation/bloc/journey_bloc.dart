import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/journey_repository.dart';
import 'journey_event.dart';
import 'journey_state.dart';

class JourneyBloc extends Bloc<JourneyEvent, JourneyState> {
  final JourneyRepository repository;

  JourneyBloc({required this.repository}) : super(JourneyInitial()) {
    on<FetchJourneyRecommendations>(_onFetch);
  }

  Future<void> _onFetch(FetchJourneyRecommendations event, Emitter<JourneyState> emit) async {
    emit(JourneyLoading());
    try {
      final data = await repository.recommend(
        city: event.city,
        guests: event.guests,
        maxBudget: event.maxBudget,
      );
      emit(JourneyLoaded(data));
    } catch (e) {
      emit(JourneyError(e.toString()));
    }
  }
}
