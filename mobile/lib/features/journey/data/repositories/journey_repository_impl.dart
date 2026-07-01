import '../../../../core/api/api_client.dart';
import '../../domain/repositories/journey_repository.dart';
import '../models/journey_item_model.dart';

class JourneyRepositoryImpl implements JourneyRepository {
  final ApiClient apiClient;

  JourneyRepositoryImpl({required this.apiClient});

  @override
  Future<JourneyRecommendationModel> recommend({
    required String city,
    required int guests,
    double? maxBudget,
  }) async {
    final params = <String, dynamic>{'city': city, 'guests': guests};
    if (maxBudget != null) params['maxBudget'] = maxBudget;
    final response = await apiClient.get('/journeys/recommend', queryParameters: params);
    final data = response.data['data'] as Map<String, dynamic>? ?? {};
    return JourneyRecommendationModel.fromJson(data);
  }
}
