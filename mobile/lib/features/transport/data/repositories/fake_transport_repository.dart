import 'dart:async';
import '../../domain/repositories/transport_repository.dart';
import '../models/transport_model.dart';

class FakeTransportRepository implements TransportRepository {
  @override
  Future<List<TransportModel>> getTransportTypes() async {
    await Future.delayed(const Duration(milliseconds: 500));
    return [
      const TransportModel(id: '1', type: 'اقتصادية', basePrice: 25000, etaMinutes: 5),
      const TransportModel(id: '2', type: 'VIP', basePrice: 50000, etaMinutes: 3),
      const TransportModel(id: '3', type: 'Van', basePrice: 75000, etaMinutes: 8),
    ];
  }

  @override
  Future<String> bookTransport(String from, String to, String type) async {
    await Future.delayed(const Duration(seconds: 1));
    return 'booking_123';
  }

  @override
  Stream<DriverModel> trackDriver(String bookingId) async* {
    for (int i = 5; i >= 0; i--) {
      await Future.delayed(const Duration(seconds: 2));
      yield DriverModel(
        id: 'd1',
        name: 'محمد الأحمد',
        carModel: 'KIA Cerato',
        carColor: 'أبيض',
        plateNumber: '123456 دمشق',
        lat: 33.5138,
        lng: 36.2765,
        etaMinutes: i,
      );
    }
  }
}
