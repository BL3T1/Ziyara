import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:share_plus/share_plus.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/services/favorites_service.dart';
import '../../data/models/hotel_model.dart';

class HotelHeaderSection extends StatelessWidget {
  final HotelModel hotel;
  const HotelHeaderSection({super.key, required this.hotel});

  @override
  Widget build(BuildContext context) {
    return SliverAppBar(
      expandedHeight: 300,
      pinned: true,
      backgroundColor: AppColors.primaryBlue,
      leading: CircleAvatar(
        backgroundColor: Colors.black38,
        child: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      actions: [
        CircleAvatar(
          backgroundColor: Colors.black38,
          child: IconButton(
            icon: const Icon(Icons.share, color: Colors.white),
            onPressed: () => Share.share('انظر لهذا الفندق الرائع: ${hotel.name}\nبسعر: ${hotel.pricePerNight} ل.س'),
          ),
        ),
        const SizedBox(width: 10),
        ValueListenableBuilder<List<String>>(
          valueListenable: FavoritesService.favoritesNotifier,
          builder: (context, favList, child) {
            final isFav = favList.contains(hotel.id);
            return CircleAvatar(
              backgroundColor: Colors.black38,
              child: IconButton(
                icon: Icon(isFav ? Icons.favorite : Icons.favorite_border, color: isFav ? Colors.red : Colors.white),
                onPressed: () => FavoritesService.toggleFavorite(hotel.id),
              ),
            );
          },
        ),
        const SizedBox(width: 10),
      ],
      flexibleSpace: FlexibleSpaceBar(
        background: Hero(
          tag: hotel.id,
          child: CachedNetworkImage(imageUrl: hotel.imageUrl, fit: BoxFit.cover),
        ),
      ),
    );
  }
}
