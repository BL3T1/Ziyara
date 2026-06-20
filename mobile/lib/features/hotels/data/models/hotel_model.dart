import '../../../../core/utils/dummy_image_manager.dart';

class HotelModel {
  final String id;
  final String name;
  final String location;
  final String address;
  final double rating;
  final int stars;
  final double pricePerNight;
  final String imageUrl;
  final List<String> facilities;

  HotelModel({required this.id, required this.name, required this.location, required this.address, required this.rating, required this.stars, required this.pricePerNight, required this.imageUrl, required this.facilities});

  factory HotelModel.fromJson(Map<String, dynamic> json) {
    return HotelModel(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      location: json['city'] as String? ?? json['location'] as String? ?? '',
      address: json['address'] as String? ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      stars: (json['stars'] as num?)?.toInt() ?? (json['category'] as num?)?.toInt() ?? 0,
      pricePerNight: (json['pricePerNight'] as num?)?.toDouble() ??
          (json['basePrice'] as num?)?.toDouble() ?? 0.0,
      imageUrl: (json['imageUrl'] as String? ?? json['thumbnailUrl'] as String? ?? '').isNotEmpty
          ? (json['imageUrl'] as String? ?? json['thumbnailUrl'] as String?)!
          : DummyImageManager.hotelSheraton,
      facilities: (json['facilities'] as List?)?.map((e) => e.toString()).toList() ??
          (json['amenities'] as List?)?.map((e) => e.toString()).toList() ?? [],
    );
  }
  
  static List<HotelModel> get dummyHotels => [
    HotelModel(id: '1', name: 'فندق شيراتون دمشق', location: 'دمشق', address: 'ساحة الأمويين', rating: 4.8, stars: 5, pricePerNight: 2500000, 
      imageUrl: DummyImageManager.hotelSheraton, // استخدام المتغير
      facilities: ['مسبح خارجي', 'نادي رياضي', 'إنترنت سريع', 'مطعم شرقي']),
    HotelModel(id: '2', name: 'منتجع لاميرا (الميريديان)', location: 'اللاذقية', address: 'الكورنيش الغربي', rating: 4.6, stars: 5, pricePerNight: 1800000, 
      imageUrl: DummyImageManager.hotelLamera,
      facilities: ['شاطئ خاص', 'إطلالة بحرية', 'ملاعب تنس', 'تراس صيفي']),
    HotelModel(id: '3', name: 'فندق الداما روز', location: 'دمشق', address: 'شارع شكري القوتلي', rating: 4.7, stars: 5, pricePerNight: 2200000, 
      imageUrl: DummyImageManager.hotelDamaRose,
      facilities: ['قاعات مؤتمرات', 'سبا ومساج', 'بار', 'خدمة رجال أعمال']),
    HotelModel(id: '4', name: 'فندق شاهين تاور', location: 'طرطوس', address: 'الكورنيش البحري', rating: 4.3, stars: 4, pricePerNight: 950000, 
      imageUrl: DummyImageManager.hotelShaheen,
      facilities: ['إفطار مجاني', 'مواقف سيارات', 'إطلالة بانورامية']),
    HotelModel(id: '5', name: 'بيت الوالي', location: 'دمشق القديمة', address: 'باب توما', rating: 4.9, stars: 4, pricePerNight: 1200000, 
      imageUrl: DummyImageManager.hotelBeitWali,
      facilities: ['أجواء تراثية', 'فطور دمشقي', 'ساحة سماوية']),
  ];
}
