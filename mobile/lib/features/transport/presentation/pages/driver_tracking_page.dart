import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/di/injection_container.dart';
import '../../domain/repositories/transport_repository.dart';
import '../bloc/transport_bloc.dart';
import '../bloc/transport_event.dart';
import '../bloc/transport_state.dart';

class DriverTrackingPage extends StatelessWidget {
  final String? bookingId;
  const DriverTrackingPage({super.key, this.bookingId});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => TransportBloc(repository: sl<TransportRepository>())
        ..add(StartTracking(bookingId ?? '')),
      child: const DriverTrackingView(),
    );
  }
}

class DriverTrackingView extends StatelessWidget {
  const DriverTrackingView({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          _buildMapBackground(),
          Positioned(
            top: 50,
            right: 20,
            child: CircleAvatar(
              backgroundColor: Colors.white,
              child: IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.black),
                onPressed: () => context.go('/home'),
              ),
            ),
          ),
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: _buildDriverSheet(),
          ),
        ],
      ),
    );
  }

  Widget _buildMapBackground() {
    return Container(
      color: const Color(0xFFE5E5E5),
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.map_outlined, size: 100, color: Colors.grey[400]),
            const SizedBox(height: 10),
            Text('خريطة التتبع', style: TextStyle(color: Colors.grey[600], fontSize: 20)),
          ],
        ),
      ),
    );
  }

  Widget _buildDriverSheet() {
    return BlocBuilder<TransportBloc, TransportState>(
      builder: (context, state) {
        if (state is TransportTrackingUpdate) {
          final driver = state.driver;
          return Container(
            padding: const EdgeInsets.all(24),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
              boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 20)],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'السائق في الطريق إليك',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.primaryBlue),
                ),
                const SizedBox(height: 4),
                Text(
                  driver.etaMinutes > 0 ? 'يصل خلال ${driver.etaMinutes} دقائق' : 'وصل السائق!',
                  style: TextStyle(color: driver.etaMinutes > 0 ? AppColors.success : AppColors.primaryBlue, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 20),
                Row(
                  children: [
                    const CircleAvatar(
                      radius: 30,
                      backgroundColor: AppColors.background,
                      child: Icon(Icons.person, size: 30, color: AppColors.textGrey),
                    ),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(driver.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                        Text('${driver.carModel} • ${driver.carColor}', style: const TextStyle(color: AppColors.textGrey)),
                      ],
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.black),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(driver.plateNumber, style: const TextStyle(fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: () {},
                        icon: const Icon(Icons.call),
                        label: const Text('اتصال'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primaryDark,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () {},
                        icon: const Icon(Icons.message),
                        label: const Text('رسالة'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.primaryDark,
                          side: const BorderSide(color: AppColors.primaryDark),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          );
        }
        return const SizedBox.shrink();
      },
    );
  }
}
