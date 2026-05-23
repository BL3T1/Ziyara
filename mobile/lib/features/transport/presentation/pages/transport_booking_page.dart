import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../bloc/transport_bloc.dart';
import '../bloc/transport_event.dart';
import '../bloc/transport_state.dart';
import '../../data/repositories/fake_transport_repository.dart';

class TransportBookingPage extends StatelessWidget {
  const TransportBookingPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => TransportBloc(
        repository: FakeTransportRepository(),
      )..add(FetchTransportTypes()),
      child: const TransportBookingView(),
    );
  }
}

class TransportBookingView extends StatefulWidget {
  const TransportBookingView({super.key});

  @override
  State<TransportBookingView> createState() => _TransportBookingViewState();
}

class _TransportBookingViewState extends State<TransportBookingView> {
  final _fromController = TextEditingController();
  final _toController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return BlocListener<TransportBloc, TransportState>(
      listener: (context, state) {
        if (state is TransportBookingSuccess) {
          context.push('/driver_tracking', extra: state.bookingId);
        } else if (state is TransportError) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.message)));
        }
      },
      child: Scaffold(
        resizeToAvoidBottomInset: false,
        body: Stack(
          children: [
            _buildMapBackground(),
            Positioned(
              top: 50, right: 20,
              child: CircleAvatar(
                backgroundColor: Colors.white,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back),
                  onPressed: () => context.go('/home'),
                ),
              ),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: _buildBookingSheet(),
            ),
            BlocBuilder<TransportBloc, TransportState>(
              builder: (context, state) {
                if (state is TransportLoading) {
                  return Container(
                    color: Colors.black26,
                    child: const Center(child: CircularProgressIndicator()),
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapBackground() {
    return Container(
      height: MediaQuery.of(context).size.height * 0.6,
      color: const Color(0xFFF0F0F0),
      child: const Center(child: Opacity(opacity: 0.1, child: Icon(Icons.map, size: 200, color: Colors.black))),
    );
  }

  Widget _buildBookingSheet() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
        boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 20)],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('إلى أين تريد الذهاب؟', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryBlue)),
          const SizedBox(height: 20),
          _buildLocationInput(_fromController, 'موقع الانطلاق', Icons.my_location),
          const SizedBox(height: 10),
          _buildLocationInput(_toController, 'الوجهة', Icons.location_on),
          const SizedBox(height: 20),
          const Text('نوع السيارة', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 10),
          BlocBuilder<TransportBloc, TransportState>(
            builder: (context, state) {
              if (state is TransportLoaded) {
                return Row(
                  children: state.types.map((type) => _buildCarOption(type.type, _getIconForType(type.type), state.selectedType == type.type)).toList(),
                );
              }
              return const SizedBox(height: 60);
            },
          ),
          const SizedBox(height: 24),
          _buildPriceAndButton(),
        ],
      ),
    );
  }

  IconData _getIconForType(String type) {
    switch (type) {
      case 'VIP': return Icons.star;
      case 'Van': return Icons.airport_shuttle;
      default: return Icons.directions_car;
    }
  }

  Widget _buildLocationInput(TextEditingController ctrl, String hint, IconData icon) {
    return TextField(
      controller: ctrl,
      decoration: InputDecoration(
        prefixIcon: Icon(icon, color: AppColors.gold),
        hintText: hint,
        filled: true,
        fillColor: AppColors.background,
        border: OutlineInputBorder(borderSide: BorderSide.none, borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  Widget _buildCarOption(String title, IconData icon, bool isSelected) {
    return Expanded(
      child: GestureDetector(
        onTap: () {
          context.read<TransportBloc>().add(SelectCarType(title));
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: isSelected ? AppColors.primaryDark : Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: isSelected ? AppColors.primaryDark : Colors.grey.shade300),
          ),
          child: Column(
            children: [
              Icon(icon, color: isSelected ? AppColors.gold : AppColors.textGrey),
              const SizedBox(height: 4),
              Text(title, style: TextStyle(color: isSelected ? Colors.white : AppColors.textGrey, fontSize: 12, fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPriceAndButton() {
    return BlocBuilder<TransportBloc, TransportState>(
      builder: (context, state) {
        double price = 0;
        String selectedType = '';
        if (state is TransportLoaded) {
          price = state.estimatedPrice;
          selectedType = state.selectedType;
        }
        return Row(
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('السعر التقديري', style: TextStyle(color: AppColors.textGrey, fontSize: 12)),
                Text(price == 0 ? '--' : '${price.toInt()} ل.س', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.primaryBlue)),
              ],
            ),
            const Spacer(),
            ElevatedButton(
              onPressed: price == 0 ? null : () {
                context.read<TransportBloc>().add(BookTransport(
                  from: _fromController.text,
                  to: _toController.text,
                  type: selectedType,
                ));
              },
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryDark, padding: const EdgeInsets.symmetric(horizontal: 40, vertical: 16)),
              child: const Text('اطلب الآن'),
            ),
          ],
        );
      },
    );
  }
}
