import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';

class CurrencyExchangeSheet extends StatelessWidget {
  const CurrencyExchangeSheet({super.key});

  @override
  Widget build(BuildContext context) {
    // استخدام DraggableScrollableSheet يجعل القائمة مرنة جداً
    return DraggableScrollableSheet(
      initialChildSize: 0.5, // تبدأ بنصف الشاشة
      minChildSize: 0.3,
      maxChildSize: 0.85,
      expand: false,
      builder: (context, scrollController) {
        return Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(25)),
          ),
          child: Column(
            children: [
              const SizedBox(height: 15),
              // مقبض السحب
              Container(
                width: 50, height: 5,
                decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(10)),
              ),
              const SizedBox(height: 20),
              const Text('أسعار الصرف المباشرة', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryBlue)),
              const SizedBox(height: 10),
              const Divider(),

              // القائمة
              Expanded(
                child: ListView(
                  controller: scrollController, // ربط السكرول
                  padding: const EdgeInsets.all(20),
                  children: [
                    _buildRow('💲', 'دولار أمريكي', '14,500'),
                    _buildRow('💶', 'يورو', '15,800'),
                    _buildRow('💵', 'درهم إماراتي', '3,950'),
                    _buildRow('💰', 'ريال سعودي', '3,860'),
                    _buildRow('💰', 'دينار أردني', '20,400'),
                    _buildRow('💰', 'جنيه إسترليني', '18,200'),
                    _buildRow('💰', 'ليرة تركية', '450'),
                    _buildRow('💰', 'دينار كويتي', '47,100'),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildRow(String flag, String name, String price) {
    return Container(
      margin: const EdgeInsets.only(bottom: 15),
      padding: const EdgeInsets.symmetric(vertical: 15, horizontal: 15),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(children: [
            Text(flag, style: const TextStyle(fontSize: 24)),
            const SizedBox(width: 15),
            Text(name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ]),
          Text('$price ل.س', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: AppColors.primaryBlue)),
        ],
      ),
    );
  }
}
