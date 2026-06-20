import '../../../../core/api/api_client.dart';
import '../models/exchange_rate_model.dart';

class ExchangeRateRepositoryImpl {
  final ApiClient apiClient;

  ExchangeRateRepositoryImpl({required this.apiClient});

  /// Fetches current exchange rates from the backend.
  /// Returns a map of currency code → rate relative to [baseCurrency].
  Future<Map<String, double>> getRates({String baseCurrency = 'USD'}) async {
    final response = await apiClient.get(
      '/exchange-rates',
      queryParameters: {'base': baseCurrency},
    );
    final raw = response.data['data'];
    if (raw is Map) {
      final result = <String, double>{};
      raw.forEach((key, value) {
        if (value is num) result[key.toString()] = value.toDouble();
      });
      return result;
    }
    // Fallback: try list format [{ fromCurrency, toCurrency, rate }]
    if (raw is List) {
      final result = <String, double>{};
      for (final item in raw) {
        if (item is Map &&
            item['fromCurrency'] == baseCurrency) {
          result[item['toCurrency'].toString()] = (item['rate'] as num).toDouble();
        }
      }
      return result;
    }
    return {};
  }
}
