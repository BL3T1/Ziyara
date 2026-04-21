import '../../../../core/api/api_client.dart';
import '../../domain/repositories/booking_repository.dart';
import '../models/booking_model.dart';

class BookingRepositoryImpl implements BookingRepository {
  final ApiClient apiClient;

  BookingRepositoryImpl({required this.apiClient});

  @override
  Future<List<BookingModel>> getMyBookings() async {
    final response = await apiClient.get('/bookings/my');
    return (response.data as List).map((e) => BookingModel.fromJson(e)).toList();
  }

  @override
  Future<void> cancelBooking(String id) async {
    await apiClient.post('/bookings/$id/cancel');
  }
}
