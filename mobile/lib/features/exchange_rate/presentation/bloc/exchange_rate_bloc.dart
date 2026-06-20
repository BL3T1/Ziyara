import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../data/repositories/exchange_rate_repository_impl.dart';

// ── Events ──────────────────────────────────────────────────────────────────
abstract class ExchangeRateEvent extends Equatable {
  const ExchangeRateEvent();
  @override
  List<Object?> get props => [];
}

class FetchExchangeRates extends ExchangeRateEvent {
  final String baseCurrency;
  const FetchExchangeRates({this.baseCurrency = 'USD'});
  @override
  List<Object?> get props => [baseCurrency];
}

// ── States ───────────────────────────────────────────────────────────────────
abstract class ExchangeRateState extends Equatable {
  const ExchangeRateState();
  @override
  List<Object?> get props => [];
}

class ExchangeRateInitial extends ExchangeRateState {}

class ExchangeRateLoading extends ExchangeRateState {}

class ExchangeRateLoaded extends ExchangeRateState {
  /// Map of currency code → exchange rate (relative to base currency).
  final Map<String, double> rates;
  final String baseCurrency;

  const ExchangeRateLoaded({required this.rates, required this.baseCurrency});

  double? rateFor(String currency) => rates[currency];

  @override
  List<Object?> get props => [rates, baseCurrency];
}

class ExchangeRateError extends ExchangeRateState {
  final String message;
  const ExchangeRateError(this.message);
  @override
  List<Object?> get props => [message];
}

// ── BLoC ─────────────────────────────────────────────────────────────────────
class ExchangeRateBloc extends Bloc<ExchangeRateEvent, ExchangeRateState> {
  final ExchangeRateRepositoryImpl repository;

  ExchangeRateBloc({required this.repository}) : super(ExchangeRateInitial()) {
    on<FetchExchangeRates>(_onFetch);
  }

  Future<void> _onFetch(
    FetchExchangeRates event,
    Emitter<ExchangeRateState> emit,
  ) async {
    emit(ExchangeRateLoading());
    try {
      final rates = await repository.getRates(baseCurrency: event.baseCurrency);
      emit(ExchangeRateLoaded(rates: rates, baseCurrency: event.baseCurrency));
    } catch (e) {
      // On error fall back silently — the UI will show cached/last-known rate
      emit(ExchangeRateError(e.toString()));
    }
  }
}
