import '../../domain/repositories/tours_repository.dart';
import '../../data/models/tour_model.dart';

class FakeToursRepository implements ToursRepository {
  @override
  Future<List<TourModel>> getTours() async {
    await Future.delayed(const Duration(seconds: 2));
    return TourModel.dummyTours;
  }
}
