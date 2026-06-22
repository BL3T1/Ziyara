import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/di/injection_container.dart';
import '../../../exchange_rate/presentation/bloc/exchange_rate_bloc.dart';
import '../../../exchange_rate/data/repositories/exchange_rate_repository_impl.dart';

/// Displays the live USD exchange rate fetched from the backend.
/// Wraps itself in a [BlocProvider] so it can be dropped anywhere in the tree.
class ExchangeRateBar extends StatelessWidget {
  const ExchangeRateBar({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => ExchangeRateBloc(
        repository: sl<ExchangeRateRepositoryImpl>(),
      )..add(const FetchExchangeRates(baseCurrency: 'USD')),
      child: const _ExchangeRateBarView(),
    );
  }
}

class _ExchangeRateBarView extends StatelessWidget {
  const _ExchangeRateBarView();

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<ExchangeRateBloc, ExchangeRateState>(
      builder: (context, state) {
        // Determine the SYP rate to display
        String rateText = '…';
        IconData trendIcon = Icons.remove;
        Color trendColor = Colors.white70;

        if (state is ExchangeRateLoaded) {
          // Try SYP first, fall back to SAR then EUR
          final syp = state.rateFor('SYP');
          final sar = state.rateFor('SAR');
          if (syp != null) {
            rateText = '1 USD = ${syp.toStringAsFixed(0)} SYP';
          } else if (sar != null) {
            rateText = '1 USD = ${sar.toStringAsFixed(2)} SAR';
          } else if (state.rates.isNotEmpty) {
            final first = state.rates.entries.first;
            rateText = '1 USD = ${first.value.toStringAsFixed(2)} ${first.key}';
          }
          trendIcon = Icons.arrow_drop_down;
          trendColor = Colors.red[300]!;
        } else if (state is ExchangeRateError) {
          rateText = 'غير متاح';
        }

        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
          decoration: BoxDecoration(
            color: AppColors.primaryBlue,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            children: [
              const Icon(Icons.currency_exchange, color: AppColors.gold, size: 20),
              const SizedBox(width: 8),
              const Text(
                'الصرف',
                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
              ),
              const Spacer(),
              if (state is ExchangeRateLoading)
                const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    color: AppColors.gold,
                    strokeWidth: 2,
                  ),
                )
              else
                Flexible(
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Flexible(
                        child: Text(
                          rateText,
                          style: const TextStyle(
                            color: AppColors.gold,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                          overflow: TextOverflow.ellipsis,
                          maxLines: 1,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Icon(trendIcon, color: trendColor, size: 20),
                    ],
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
