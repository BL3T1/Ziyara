import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../../core/theme/app_colors.dart';
import '../../data/models/tour_model.dart';

class TourDetailsPage extends StatelessWidget {
  final TourModel tour;
  const TourDetailsPage({super.key, required this.tour});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 300,
            pinned: true,
            leading: CircleAvatar(backgroundColor: Colors.black38, child: IconButton(icon: const Icon(Icons.arrow_back, color: Colors.white), onPressed: () => Navigator.pop(context))),
            flexibleSpace: FlexibleSpaceBar(
              background: Hero(tag: tour.id, child: CachedNetworkImage(imageUrl: tour.imageUrl, fit: BoxFit.cover)),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(24.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeader(),
                  const SizedBox(height: 24),
                  _buildDescription(),
                  const SizedBox(height: 24),
                  _buildActivitiesList(),
                  const SizedBox(height: 100),
                ],
              ),
            ),
          ),
        ],
      ),
      bottomSheet: _buildBottomBar(context),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Expanded(child: Text(tour.title, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.primaryBlue))),
          Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6), decoration: BoxDecoration(color: AppColors.primaryDark, borderRadius: BorderRadius.circular(20)), child: Text('${tour.durationDays} أيام', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold))),
        ]),
        const SizedBox(height: 8),
        Row(children: [const Icon(Icons.location_on, color: AppColors.gold, size: 20), const SizedBox(width: 4), Text(tour.location, style: const TextStyle(fontSize: 16, color: AppColors.textGrey))]),
      ],
    );
  }

  Widget _buildDescription() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('عن الرحلة', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
      const SizedBox(height: 8),
      Text(tour.description, style: const TextStyle(color: AppColors.textGrey, height: 1.6)),
    ]);
  }

  Widget _buildActivitiesList() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Text('النشاطات', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
      const SizedBox(height: 12),
      ...tour.activities.map((a) => Padding(padding: const EdgeInsets.only(bottom: 10), child: Row(children: [const Icon(Icons.check_circle_outline, color: AppColors.success), const SizedBox(width: 10), Text(a)]))),
    ]);
  }

  Widget _buildBottomBar(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(color: Colors.white, boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10, offset: const Offset(0, -5))]),
      child: Row(children: [
        Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('السعر للشخص', style: TextStyle(color: AppColors.textGrey)), Text('${tour.price.toInt()} ل.س', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryBlue))]),
        const Spacer(),
        ElevatedButton(
          onPressed: () => context.push("/payment", extra: {"title": tour.title, "price": tour.price, "imageUrl": tour.imageUrl}),
          style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryDark, padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 16)),
          child: const Text('حجز مقعد'),
        ),
      ]),
    );
  }
}
