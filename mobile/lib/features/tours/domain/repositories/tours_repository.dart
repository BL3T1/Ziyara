import '../../data/models/tour_model.dart';

abstract class ToursRepository {
  Future<List<TourModel>> getTours();
}
