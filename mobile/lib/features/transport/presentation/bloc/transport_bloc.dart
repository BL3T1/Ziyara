import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/transport_repository.dart';
import '../../data/models/transport_model.dart';
import 'transport_event.dart';
import 'transport_state.dart';

class TransportBloc extends Bloc<TransportEvent, TransportState> {
  final TransportRepository repository;
  StreamSubscription<DriverModel>? _trackingSubscription;

  TransportBloc({required this.repository}) : super(TransportInitial()) {
    on<FetchTransportTypes>(_onFetchTransportTypes);
    on<SelectCarType>(_onSelectCarType);
    on<BookTransport>(_onBookTransport);
    on<StartTracking>(_onStartTracking);
    on<_InternalTrackingUpdate>(_onInternalTrackingUpdate);
  }

  Future<void> _onFetchTransportTypes(FetchTransportTypes event, Emitter<TransportState> emit) async {
    emit(TransportLoading());
    try {
      final types = await repository.getTransportTypes();
      emit(TransportLoaded(
        types: types,
        selectedType: types.first.type,
        estimatedPrice: types.first.basePrice + 15000,
      ));
    } catch (e) {
      emit(TransportError(e.toString()));
    }
  }

  void _onSelectCarType(SelectCarType event, Emitter<TransportState> emit) {
    if (state is TransportLoaded) {
      final currentState = state as TransportLoaded;
      final selected = currentState.types.firstWhere((t) => t.type == event.carType);
      emit(TransportLoaded(
        types: currentState.types,
        selectedType: event.carType,
        estimatedPrice: selected.basePrice + 15000,
      ));
    }
  }

  Future<void> _onBookTransport(BookTransport event, Emitter<TransportState> emit) async {
    emit(TransportLoading());
    try {
      final bookingId = await repository.bookTransport(event.from, event.to, event.type);
      emit(TransportBookingSuccess(bookingId));
    } catch (e) {
      emit(TransportError(e.toString()));
    }
  }

  Future<void> _onStartTracking(StartTracking event, Emitter<TransportState> emit) async {
    await _trackingSubscription?.cancel();
    _trackingSubscription = repository.trackDriver(event.bookingId).listen(
      (driver) => add(_InternalTrackingUpdate(driver)),
    );
  }

  void _onInternalTrackingUpdate(_InternalTrackingUpdate event, Emitter<TransportState> emit) {
    emit(TransportTrackingUpdate(event.driver));
  }

  @override
  Future<void> close() {
    _trackingSubscription?.cancel();
    return super.close();
  }
}

class _InternalTrackingUpdate extends TransportEvent {
  final DriverModel driver;
  const _InternalTrackingUpdate(this.driver);

  @override
  List<Object?> get props => [driver];
}
