import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/widgets/shimmer/tour_card_shimmer.dart';
import '../../../../core/di/injection_container.dart';
import '../../domain/repositories/tours_repository.dart';
import '../widgets/tour_card.dart';
import 'tour_details_page.dart';
import '../bloc/tour_bloc.dart';
import '../bloc/tour_event.dart';
import '../bloc/tour_state.dart';

class ToursPage extends StatelessWidget {
  const ToursPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => TourBloc(repository: sl<ToursRepository>())..add(FetchTours()),
      child: const ToursView(),
    );
  }
}

class ToursView extends StatelessWidget {
  const ToursView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('الرحلات السياحية'),
        backgroundColor: AppColors.background,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<TourBloc>().add(FetchTours()),
          )
        ],
      ),
      body: BlocBuilder<TourBloc, TourState>(
        builder: (context, state) {
          if (state is TourLoading || state is TourInitial) {
            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: 3,
              itemBuilder: (context, index) => const TourCardShimmer(),
            );
          } else if (state is TourError) {
            return Center(child: Text(state.message));
          } else if (state is TourLoaded) {
            if (state.tours.isEmpty) {
              return const Center(child: Text('لا توجد رحلات سياحية حالياً'));
            }
            return ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: state.tours.length,
              itemBuilder: (context, index) {
                final tour = state.tours[index];
                return TourCard(
                  tour: tour,
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => TourDetailsPage(tour: tour)),
                    );
                  },
                );
              },
            );
          }
          return const SizedBox.shrink();
        },
      ),
    );
  }
}
