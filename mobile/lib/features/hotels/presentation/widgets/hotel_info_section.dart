import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../data/models/hotel_model.dart';

class HotelInfoSection extends StatelessWidget {
  final HotelModel hotel;
  const HotelInfoSection({super.key, required this.hotel});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(child: Text(hotel.name, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppColors.primaryBlue))),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: AppColors.gold.withOpacity(0.2), borderRadius: BorderRadius.circular(8)),
                child: Row(children: [const Icon(Icons.star, color: AppColors.gold, size: 16), const SizedBox(width: 4), Text('${hotel.rating}', style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primaryDark))]),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(children: [const Icon(Icons.location_on, color: AppColors.gold, size: 18), const SizedBox(width: 4), Text(hotel.address, style: const TextStyle(color: AppColors.textGrey))]),
          
          const SizedBox(height: 24),
          
          // --- قسم الميزات والخدمات المعاد تصميمه ---
          const Text('الميزات والخدمات المتوفرة', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: hotel.facilities.map((feature) => Chip(
              label: Text(feature, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
              avatar: const Icon(Icons.check_circle, color: AppColors.success, size: 18),
              backgroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade300)),
            )).toList(),
          ),
          
          const SizedBox(height: 24),
          const Text('نبذة عن المكان', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          const Text('استمتع بإقامة فاخرة مع أفضل الخدمات. الموقع قريب من المراكز الحيوية ومجهز بكافة وسائل الراحة لضمان تجربة لا تنسى.', style: TextStyle(color: AppColors.textGrey, height: 1.5)),
          const SizedBox(height: 100),
        ],
      ),
    );
  }
}
