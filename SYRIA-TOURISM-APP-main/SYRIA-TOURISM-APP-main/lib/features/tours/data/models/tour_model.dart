import '../../../../core/utils/dummy_image_manager.dart';

class TourModel {
  final String id;
  final String title;
  final String location;
  final double price;
  final int durationDays;
  final double rating;
  final String imageUrl;
  final String description;
  final List<String> activities;
  final String date;

  TourModel({required this.id, required this.title, required this.location, required this.price, required this.durationDays, required this.rating, required this.imageUrl, required this.description, required this.activities, required this.date});

  static List<TourModel> get dummyTours => [
    TourModel(id: '1', title: 'رحلة القلاع والحصون', location: 'وادي النصارى - الحصن', price: 450000, durationDays: 2, rating: 4.8, 
      imageUrl: DummyImageManager.tourKrak,
      description: 'زيارة قلعة الحصن الأثرية والاستمتاع بالطبيعة الخلابة.', activities: ['زيارة القلعة', 'غداء في الوادي', 'سهرة فنية'], date: 'كل خميس'),
    TourModel(id: '2', title: 'سحر الطبيعة في كسب', location: 'اللاذقية - كسب', price: 600000, durationDays: 3, rating: 4.9, 
      imageUrl: DummyImageManager.tourKassab,
      description: 'استمتع بأجواء الغابات والجبال في كسب والسمرا.', activities: ['تخييم', 'مشوي', 'سباحة في السمرا'], date: '20 آب 2025'),
    TourModel(id: '3', title: 'جولة تدمر التاريخية', location: 'حمص - تدمر', price: 350000, durationDays: 1, rating: 4.7, 
      imageUrl: DummyImageManager.tourPalmyra,
      description: 'رحلة عبر الزمن لزيارة آثار تدمر العريقة.', activities: ['زيارة الآثار', 'غداء بدوي', 'ركوب الجمال'], date: 'يومياً'),
  ];
}
