import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../pages/select_room_page.dart';

class HotelBookingBar extends StatelessWidget {
  final String hotelName;
  final double price;
  final String imageUrl;

  const HotelBookingBar({
    super.key,
    required this.hotelName,
    required this.price,
    required this.imageUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 10, offset: const Offset(0, -5))],
      ),
      child: Row(
        children: [
          Expanded(
            flex: 2,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('السعر لليلة', style: TextStyle(color: AppColors.textGrey, fontSize: 12)),
                Text('${price.toInt()} ل.س', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryBlue)),
              ],
            ),
          ),
          Expanded(
            flex: 3,
            child: ElevatedButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => SelectRoomPage(hotelName: hotelName, price: price, imageUrl: imageUrl),
                  ),
                );
              },
              style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryDark, padding: const EdgeInsets.symmetric(vertical: 16)),
              child: const Text('اختيار الغرفة'),
            ),
          ),
        ],
      ),
    );
  }
}
