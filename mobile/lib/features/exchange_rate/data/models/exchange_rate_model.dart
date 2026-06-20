// Placeholder — rates are surfaced as Map<String, double> directly from the repo.
// Add this model if pagination or richer data is needed later.
class ExchangeRateModel {
  final String fromCurrency;
  final String toCurrency;
  final double rate;

  const ExchangeRateModel({
    required this.fromCurrency,
    required this.toCurrency,
    required this.rate,
  });

  factory ExchangeRateModel.fromJson(Map<String, dynamic> json) {
    return ExchangeRateModel(
      fromCurrency: json['fromCurrency'] as String? ?? '',
      toCurrency: json['toCurrency'] as String? ?? '',
      rate: (json['rate'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
