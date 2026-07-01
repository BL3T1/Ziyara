class JourneyItemModel {
  final String id;
  final String name;
  final String city;
  final double? basePrice;
  final String currency;
  final String imageUrl;
  final String type;

  JourneyItemModel({
    required this.id,
    required this.name,
    required this.city,
    this.basePrice,
    required this.currency,
    required this.imageUrl,
    required this.type,
  });

  factory JourneyItemModel.fromJson(Map<String, dynamic> json) {
    return JourneyItemModel(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      city: json['city'] as String? ?? json['location'] as String? ?? '',
      basePrice: (json['basePrice'] as num?)?.toDouble(),
      currency: json['currency'] as String? ?? 'USD',
      imageUrl: json['imageUrl'] as String? ?? json['thumbnailUrl'] as String? ?? '',
      type: json['type'] as String? ?? '',
    );
  }
}

class JourneyRecommendationModel {
  final List<JourneyItemModel> hotels;
  final List<JourneyItemModel> taxis;
  final List<JourneyItemModel> restaurants;

  JourneyRecommendationModel({
    required this.hotels,
    required this.taxis,
    required this.restaurants,
  });

  factory JourneyRecommendationModel.fromJson(Map<String, dynamic> json) {
    List<JourneyItemModel> parse(String key) =>
        ((json[key] as List?) ?? [])
            .map((e) => JourneyItemModel.fromJson(e as Map<String, dynamic>))
            .toList();
    return JourneyRecommendationModel(
      hotels: parse('hotels'),
      taxis: parse('taxis'),
      restaurants: parse('restaurants'),
    );
  }
}
