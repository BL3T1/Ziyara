import '../../data/models/booking_model.dart';

abstract class BookingRepository {
  Future<List<BookingModel>> getMyBookings();
  Future<void> cancelBooking(String id);
}
