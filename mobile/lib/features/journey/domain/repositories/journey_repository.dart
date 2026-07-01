import '../../data/models/journey_item_model.dart';

abstract class JourneyRepository {
  Future<JourneyRecommendationModel> recommend({
    required String city,
    required int guests,
    double? maxBudget,
  });
}
