import 'package:equatable/equatable.dart';

class TransportModel extends Equatable {
  final String id;
  final String type;
  final double basePrice;
  final int etaMinutes;

  const TransportModel({
    required this.id,
    required this.type,
    required this.basePrice,
    required this.etaMinutes,
  });

  factory TransportModel.fromJson(Map<String, dynamic> json) {
    return TransportModel(
      id: json['id'],
      type: json['type'],
      basePrice: json['base_price'].toDouble(),
      etaMinutes: json['eta_minutes'],
    );
  }

  @override
  List<Object?> get props => [id, type, basePrice, etaMinutes];
}

class DriverModel extends Equatable {
  final String id;
  final String name;
  final String carModel;
  final String carColor;
  final String plateNumber;
  final double lat;
  final double lng;
  final int etaMinutes;

  const DriverModel({
    required this.id,
    required this.name,
    required this.carModel,
    required this.carColor,
    required this.plateNumber,
    required this.lat,
    required this.lng,
    required this.etaMinutes,
  });

  factory DriverModel.fromJson(Map<String, dynamic> json) {
    return DriverModel(
      id: json['id'],
      name: json['name'],
      carModel: json['car_model'],
      carColor: json['car_color'],
      plateNumber: json['plate_number'],
      lat: json['lat'].toDouble(),
      lng: json['lng'].toDouble(),
      etaMinutes: json['eta_minutes'],
    );
  }

  @override
  List<Object?> get props => [id, name, carModel, carColor, plateNumber, lat, lng, etaMinutes];
}
