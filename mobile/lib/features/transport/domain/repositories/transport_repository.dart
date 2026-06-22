import '../../data/models/transport_model.dart';

abstract class TransportRepository {
  Future<List<TransportModel>> getTransportTypes();
  Future<String> bookTransport(String from, String to, String type);
  Stream<DriverModel> trackDriver(String bookingId);
}
