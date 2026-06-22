import '../../domain/repositories/restaurants_repository.dart';
import '../../data/models/restaurant_model.dart';

class FakeRestaurantsRepository implements RestaurantsRepository {
  @override
  Future<List<RestaurantModel>> getRestaurants() async {
    // محاكاة تأخير الشبكة
    await Future.delayed(const Duration(seconds: 2));
    return RestaurantModel.dummyRestaurants;
  }
}
