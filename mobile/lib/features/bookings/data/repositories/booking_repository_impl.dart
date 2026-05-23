import '../../../../core/api/api_client.dart';
import '../../domain/repositories/booking_repository.dart';
import '../models/booking_model.dart';

class BookingRepositoryImpl implements BookingRepository {
  final ApiClient apiClient;

  BookingRepositoryImpl({required this.apiClient});

  @override
  Future<List<BookingModel>> getMyBookings() async {
    final response = await apiClient.get('/bookings/my');
    // Backend wraps responses: { "data": { "content": [...] } } or { "data": [...] }
    final raw = response.data['data'];
    final list = raw is Map ? (raw['content'] as List? ?? []) : (raw as List? ?? []);
    return list.map((e) => BookingModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  @override
  Future<void> cancelBooking(String id) async {
    await apiClient.post('/bookings/$id/cancel');
  }
}
