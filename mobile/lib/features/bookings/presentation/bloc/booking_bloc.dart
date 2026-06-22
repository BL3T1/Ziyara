import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/booking_repository.dart';
import 'booking_event.dart';
import 'booking_state.dart';

class BookingBloc extends Bloc<BookingEvent, BookingState> {
  final BookingRepository repository;

  BookingBloc({required this.repository}) : super(BookingInitial()) {
    on<FetchBookings>(_onFetchBookings);
    on<CancelBooking>(_onCancelBooking);
  }

  Future<void> _onFetchBookings(FetchBookings event, Emitter<BookingState> emit) async {
    emit(BookingLoading());
    try {
      final bookings = await repository.getMyBookings();
      final active = bookings.where((b) => b.status != 'مكتمل' && b.status != 'ملغى').toList();
      final past = bookings.where((b) => b.status == 'مكتمل' || b.status == 'ملغى').toList();
      emit(BookingLoaded(activeBookings: active, pastBookings: past));
    } catch (e) {
      emit(BookingError(e.toString()));
    }
  }

  Future<void> _onCancelBooking(CancelBooking event, Emitter<BookingState> emit) async {
    try {
      await repository.cancelBooking(event.id);
      add(FetchBookings());
    } catch (e) {
      // Handle error (maybe emit temporary error state or show snackbar)
    }
  }
}
