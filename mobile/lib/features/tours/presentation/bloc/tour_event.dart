import 'package:equatable/equatable.dart';
import '../../data/models/tour_model.dart';

abstract class TourEvent extends Equatable {
  const TourEvent();

  @override
  List<Object?> get props => [];
}

class FetchTours extends TourEvent {}

abstract class TourState extends Equatable {
  const TourState();

  @override
  List<Object?> get props => [];
}

class TourInitial extends TourState {}

class TourLoading extends TourState {}

class TourLoaded extends TourState {
  final List<TourModel> tours;

  const TourLoaded(this.tours);

  @override
  List<Object?> get props => [tours];
}

class TourError extends TourState {
  final String message;

  const TourError(this.message);

  @override
  List<Object?> get props => [message];
}
