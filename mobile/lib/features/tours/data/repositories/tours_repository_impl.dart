import '../../../../core/api/api_client.dart';
import '../../domain/repositories/tours_repository.dart';
import '../models/tour_model.dart';

class ToursRepositoryImpl implements ToursRepository {
  final ApiClient apiClient;

  ToursRepositoryImpl({required this.apiClient});

  @override
  Future<List<TourModel>> getTours() async {
    final response = await apiClient.get(
      '/services',
      queryParameters: {'type': 'TOUR', 'page': 0, 'size': 50, 'sort': 'rating,desc'},
    );
    final content = (response.data['data']['content'] as List? ?? []);
    return content.map((e) => TourModel.fromJson(e as Map<String, dynamic>)).toList();
  }
}
