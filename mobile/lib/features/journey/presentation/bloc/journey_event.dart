abstract class JourneyEvent {}

class FetchJourneyRecommendations extends JourneyEvent {
  final String city;
  final int guests;
  final double? maxBudget;

  FetchJourneyRecommendations({required this.city, required this.guests, this.maxBudget});
}
