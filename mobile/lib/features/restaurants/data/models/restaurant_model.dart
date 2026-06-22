import '../../../../core/utils/dummy_image_manager.dart';

class RestaurantModel {
  final String id;
  final String name;
  final String location;
  final String type;
  final double rating;
  final String imageUrl;
  final bool isOpenNow;
  final String workingHours;
  final List<String> menuHighlights;

  RestaurantModel({required this.id, required this.name, required this.location, required this.type, required this.rating, required this.imageUrl, required this.isOpenNow, required this.workingHours, required this.menuHighlights});

  factory RestaurantModel.fromJson(Map<String, dynamic> json) {
    return RestaurantModel(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      location: json['city'] as String? ?? json['location'] as String? ?? '',
      type: json['cuisineType'] as String? ?? json['type'] as String? ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
      imageUrl: (json['imageUrl'] as String? ?? json['thumbnailUrl'] as String? ?? '').isNotEmpty
          ? (json['imageUrl'] as String? ?? json['thumbnailUrl'] as String?)!
          : DummyImageManager.restaurantNaranj,
      isOpenNow: json['isOpenNow'] as bool? ?? json['openNow'] as bool? ?? false,
      workingHours: json['workingHours'] as String? ?? json['openingHours'] as String? ?? '',
      menuHighlights: (json['menuHighlights'] as List?)?.map((e) => e.toString()).toList() ??
          (json['highlights'] as List?)?.map((e) => e.toString()).toList() ?? [],
    );
  }
  
  static List<RestaurantModel> get dummyRestaurants => [
    RestaurantModel(id: '1', name: 'مطعم نارنج', location: 'دمشق القديمة', type: 'مأكولات شرقية فاخرة', rating: 4.9, 
      imageUrl: DummyImageManager.restaurantNaranj, 
      isOpenNow: true, workingHours: '12:00 م - 12:00 ص', menuHighlights: ['كباب كرز', 'فتة باذنجان', 'كبة مشوية']),
    RestaurantModel(id: '2', name: 'كافيه فيو (View)', location: 'اللاذقية', type: 'كافيه وإطلالة', rating: 4.5, 
      imageUrl: DummyImageManager.restaurantView,
      isOpenNow: true, workingHours: '09:00 ص - 02:00 ص', menuHighlights: ['قهوة مختصة', 'وافل', 'عصائر طبيعية']),
    RestaurantModel(id: '3', name: 'مطعم الونيش', location: 'حلب', type: 'مشويات حلبية أصيلة', rating: 4.8, 
      imageUrl: DummyImageManager.restaurantWanish,
      isOpenNow: false, workingHours: '01:00 م - 11:00 م', menuHighlights: ['كباب حلبي', 'لحم بعجين', 'سفرجلية']),
    RestaurantModel(id: '4', name: 'سكاي بار (Sky Bar)', location: 'دمشق', type: 'سهرة وعشاء', rating: 4.6, 
      imageUrl: DummyImageManager.restaurantSkyBar,
      isOpenNow: true, workingHours: '06:00 م - 03:00 ص', menuHighlights: ['ستيك', 'سوشي', 'كوكتيلات']),
  ];
}
