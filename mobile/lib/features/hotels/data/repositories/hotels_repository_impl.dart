import '../../../../core/api/api_client.dart';
import '../../domain/repositories/hotels_repository.dart';
import '../models/hotel_model.dart';

class HotelsRepositoryImpl implements HotelsRepository {
  final ApiClient apiClient;

  HotelsRepositoryImpl({required this.apiClient});

  @override
  Future<List<HotelModel>> getHotels() async {
    final response = await apiClient.get(
      '/services',
      queryParameters: {'type': 'HOTEL', 'page': 0, 'size': 50, 'sort': 'rating,desc'},
    );
    final content = (response.data['data']['content'] as List? ?? []);
    return content.map((e) => HotelModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  @override
  Future<HotelModel?> getHotelById(String id) async {
    final response = await apiClient.get('/services/$id');
    final data = response.data['data'] as Map<String, dynamic>?;
    return data != null ? HotelModel.fromJson(data) : null;
  }
}
