import 'package:flutter/material.dart';
import '../../data/models/hotel_model.dart';
import '../widgets/hotel_header_section.dart';
import '../widgets/hotel_info_section.dart';
import '../widgets/hotel_booking_bar.dart';

class HotelDetailsPage extends StatelessWidget {
  final HotelModel hotel;
  const HotelDetailsPage({super.key, required this.hotel});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          HotelHeaderSection(hotel: hotel),
          SliverToBoxAdapter(
            child: Container(
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
              ),
              transform: Matrix4.translationValues(0, -20, 0),
              child: HotelInfoSection(hotel: hotel),
            ),
          ),
        ],
      ),
      bottomSheet: HotelBookingBar(
        hotelName: hotel.name,
        price: hotel.pricePerNight,
        imageUrl: hotel.imageUrl,
      ),
    );
  }
}
