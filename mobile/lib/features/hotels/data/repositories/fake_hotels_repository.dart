import '../../domain/repositories/hotels_repository.dart';
import '../models/hotel_model.dart';

class FakeHotelsRepository implements HotelsRepository {
  @override
  Future<List<HotelModel>> getHotels() async {
    // محاكاة تأخير الشبكة (2 ثانية) لكي نرى تأثير الـ Shimmer
    await Future.delayed(const Duration(seconds: 2));
    
    // إرجاع البيانات الوهمية الموجودة في المودل
    return HotelModel.dummyHotels;
  }

  @override
  Future<HotelModel?> getHotelById(String id) async {
    await Future.delayed(const Duration(seconds: 1));
    return HotelModel.dummyHotels.firstWhere((e) => e.id == id);
  }
}
