import '../../data/models/journey_item_model.dart';

abstract class JourneyState {}

class JourneyInitial extends JourneyState {}
class JourneyLoading extends JourneyState {}
class JourneyLoaded extends JourneyState {
  final JourneyRecommendationModel data;
  JourneyLoaded(this.data);
}
class JourneyError extends JourneyState {
  final String message;
  JourneyError(this.message);
}
