class TransportOrderModel {
  final String id;
  final String fromLocation;
  final String toLocation;
  final String carType; // VIP, Economy, Van
  final double price;
  final String date;
  final String driverName; // للسائق المعين
  final String carPlate;

  TransportOrderModel({
    required this.id,
    required this.fromLocation,
    required this.toLocation,
    required this.carType,
    required this.price,
    required this.date,
    this.driverName = 'جاري التعيين...',
    this.carPlate = '---',
  });
}
