import '../../data/models/hotel_model.dart';

abstract class HotelsRepository {
  // دالة لجلب كل الفنادق
  Future<List<HotelModel>> getHotels();
  
  // دالة لجلب تفاصيل فندق محدد (للمستقبل)
  Future<HotelModel?> getHotelById(String id);
}
