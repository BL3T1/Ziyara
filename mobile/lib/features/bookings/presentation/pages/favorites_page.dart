import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/services/favorites_service.dart';
import '../../../hotels/data/models/hotel_model.dart'; // نحتاج المودل لجلب البيانات
import '../../../hotels/presentation/widgets/hotel_card.dart'; // لعرض الكرت
import '../../../hotels/presentation/pages/hotel_details_page.dart';

class FavoritesPage extends StatelessWidget {
  const FavoritesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('المفضلة'), backgroundColor: AppColors.background),
      // نستمع للتغييرات في قائمة المفضلة
      body: ValueListenableBuilder<List<String>>(
        valueListenable: FavoritesService.favoritesNotifier,
        builder: (context, favIds, child) {
          if (favIds.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.favorite_border, size: 80, color: Colors.grey.shade300),
                  const SizedBox(height: 16),
                  const Text('لم تضف شيئاً للمفضلة بعد', style: TextStyle(color: AppColors.textGrey)),
                ],
              ),
            );
          }

          // تصفية الفنادق لعرض المفضل منها فقط
          // ملاحظة: في التطبيق الحقيقي نجلب هذه البيانات من السيرفر بناءً على IDs
          final favoriteHotels = HotelModel.dummyHotels.where((h) => favIds.contains(h.id)).toList();

          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: favoriteHotels.length,
            itemBuilder: (context, index) {
              final hotel = favoriteHotels[index];
              return Dismissible( // ميزة السحب للحذف
                key: Key(hotel.id),
                direction: DismissDirection.endToStart,
                background: Container(
                  alignment: Alignment.centerLeft,
                  padding: const EdgeInsets.only(left: 20),
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(color: AppColors.error, borderRadius: BorderRadius.circular(16)),
                  child: const Icon(Icons.delete, color: Colors.white),
                ),
                onDismissed: (direction) {
                  FavoritesService.toggleFavorite(hotel.id);
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم الحذف من المفضلة')));
                },
                child: HotelCard(
                  hotel: hotel,
                  onTap: () {
                    Navigator.push(context, MaterialPageRoute(builder: (_) => HotelDetailsPage(hotel: hotel)));
                  },
                ),
              );
            },
          );
        },
      ),
    );
  }
}
