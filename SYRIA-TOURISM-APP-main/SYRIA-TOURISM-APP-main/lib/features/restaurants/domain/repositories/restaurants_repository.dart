import '../../data/models/restaurant_model.dart';

abstract class RestaurantsRepository {
  Future<List<RestaurantModel>> getRestaurants();
}
