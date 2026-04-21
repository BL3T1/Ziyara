import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FavoritesService {
  static final ValueNotifier<List<String>> favoritesNotifier = ValueNotifier([]);

  // تحميل المفضلة عند بدء التطبيق
  static Future<void> loadFavorites() async {
    final prefs = await SharedPreferences.getInstance();
    final List<String>? storedFavs = prefs.getStringList('favorite_hotels');
    if (storedFavs != null) {
      favoritesNotifier.value = storedFavs;
    }
  }

  // إضافة أو إزالة عنصر من المفضلة
  static Future<void> toggleFavorite(String hotelId) async {
    final prefs = await SharedPreferences.getInstance();
    final List<String> currentFavs = List.from(favoritesNotifier.value);
    
    if (currentFavs.contains(hotelId)) {
      currentFavs.remove(hotelId);
    } else {
      currentFavs.add(hotelId);
    }
    
    await prefs.setStringList('favorite_hotels', currentFavs);
    favoritesNotifier.value = currentFavs;
  }

  static bool isFavorite(String hotelId) {
    return favoritesNotifier.value.contains(hotelId);
  }
}
