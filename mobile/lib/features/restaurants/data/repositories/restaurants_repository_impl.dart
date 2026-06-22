import '../../../../core/api/api_client.dart';
import '../../domain/repositories/restaurants_repository.dart';
import '../models/restaurant_model.dart';

class RestaurantsRepositoryImpl implements RestaurantsRepository {
  final ApiClient apiClient;

  RestaurantsRepositoryImpl({required this.apiClient});

  @override
  Future<List<RestaurantModel>> getRestaurants() async {
    final response = await apiClient.get(
      '/services',
      queryParameters: {'type': 'RESTAURANT', 'page': 0, 'size': 50, 'sort': 'rating,desc'},
    );
    final content = (response.data['data']['content'] as List? ?? []);
    return content.map((e) => RestaurantModel.fromJson(e as Map<String, dynamic>)).toList();
  }
}
