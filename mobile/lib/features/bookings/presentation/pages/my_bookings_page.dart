import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/di/injection_container.dart';
import '../../domain/repositories/booking_repository.dart';
import '../widgets/booking_card_item.dart';
import '../bloc/booking_bloc.dart';
import '../bloc/booking_event.dart';
import '../bloc/booking_state.dart';
import '../../data/models/booking_model.dart';

class MyBookingsPage extends StatelessWidget {
  const MyBookingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => BookingBloc(repository: sl<BookingRepository>())
        ..add(FetchBookings()),
      child: const MyBookingsView(),
    );
  }
}

class MyBookingsView extends StatelessWidget {
  const MyBookingsView({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('حجوزاتي'),
          backgroundColor: AppColors.background,
          bottom: const TabBar(
            labelColor: AppColors.primaryBlue,
            unselectedLabelColor: AppColors.textGrey,
            indicatorColor: AppColors.gold,
            tabs: [Tab(text: 'الحالية'), Tab(text: 'المنتهية')],
          ),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () => context.read<BookingBloc>().add(FetchBookings()),
            ),
          ],
        ),
        body: BlocBuilder<BookingBloc, BookingState>(
          builder: (context, state) {
            if (state is BookingLoading || state is BookingInitial) {
              return const Center(child: CircularProgressIndicator());
            } else if (state is BookingError) {
              return Center(child: Text(state.message));
            } else if (state is BookingLoaded) {
              return TabBarView(
                children: [
                  _buildBookingList(context, state.activeBookings),
                  _buildBookingList(context, state.pastBookings),
                ],
              );
            }
            return const SizedBox.shrink();
          },
        ),
      ),
    );
  }

  Widget _buildBookingList(BuildContext context, List<BookingModel> bookings) {
    if (bookings.isEmpty) {
      return const Center(child: Text('لا توجد حجوزات'));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: bookings.length,
      itemBuilder: (context, index) {
        final booking = bookings[index];
        return BookingCardItem(
          title: booking.title,
          date: booking.date,
          status: booking.status,
          price: booking.price,
          canCancel: booking.canCancel,
          onCancel: () => _showCancelDialog(context, booking),
        );
      },
    );
  }

  void _showCancelDialog(BuildContext context, BookingModel booking) {
    final penalty = booking.price * 0.03;
    final refund = booking.price - penalty;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('تأكيد الإلغاء'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('هل أنت متأكد من الإلغاء؟'),
            const SizedBox(height: 10),
            Text('سيتم خصم ${penalty.toInt()} ل.س (3%)', style: const TextStyle(color: AppColors.error)),
            const Divider(),
            Text('المبلغ المسترد: ${refund.toInt()} ل.س', style: const TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('تراجع')),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(ctx);
              context.read<BookingBloc>().add(CancelBooking(booking.id));
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم الإلغاء بنجاح')));
            },
            style: ElevatedButton.styleFrom(backgroundColor: AppColors.error),
            child: const Text('نعم، إلغاء'),
          ),
        ],
      ),
    );
  }
}
