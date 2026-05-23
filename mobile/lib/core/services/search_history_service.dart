import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SearchHistoryService {
  static final ValueNotifier<List<String>> historyNotifier = ValueNotifier([]);

  // تحميل السجل
  static Future<void> loadHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final List<String>? storedHistory = prefs.getStringList('search_history');
    if (storedHistory != null) {
      historyNotifier.value = storedHistory;
    }
  }

  // إضافة كلمة بحث جديدة
  static Future<void> addSearchTerm(String term) async {
    if (term.trim().isEmpty) return;
    
    final prefs = await SharedPreferences.getInstance();
    List<String> currentHistory = List.from(historyNotifier.value);
    
    // إزالة الكلمة إذا كانت موجودة مسبقاً (لإضافتها في المقدمة)
    currentHistory.remove(term);
    
    // إضافة الكلمة في البداية
    currentHistory.insert(0, term);
    
    // الاحتفاظ بآخر 5 عمليات بحث فقط
    if (currentHistory.length > 5) {
      currentHistory = currentHistory.sublist(0, 5);
    }
    
    await prefs.setStringList('search_history', currentHistory);
    historyNotifier.value = currentHistory;
  }

  // مسح السجل
  static Future<void> clearHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('search_history');
    historyNotifier.value = [];
  }
}
