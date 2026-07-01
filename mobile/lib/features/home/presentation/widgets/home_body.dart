import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import 'home_banner_slider.dart';
import 'service_card.dart';
import 'exchange_rate_bar.dart';
import 'currency_exchange_sheet.dart' ; // <--- هذا هو السطر الذي كان ينقصك

class HomeBody extends StatelessWidget {
  const HomeBody({super.key});

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    final isTablet = width > 600;
    final crossAxisCount = isTablet ? 4 : 2;
    final childAspectRatio = isTablet ? 1.1 : 1.3;

    return ListView(
      physics: const BouncingScrollPhysics(),
      children: [
        Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 800),
            child: Column(
              children: [
                GestureDetector(
                  onTap: () {
                    showModalBottomSheet(
                      context: context,
                      builder: (c) => const CurrencyExchangeSheet(),
                      shape: const RoundedRectangleBorder(
                        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
                      ),
                    );
                  },
                  child: const ExchangeRateBar(),
                ),
                const SizedBox(height: 10),
                const HomeBannerSlider(),
              ],
            ),
          ),
        ),

        const Padding(
          padding: EdgeInsets.fromLTRB(16, 24, 16, 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('الخدمات الرقمية', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.primaryDark)),
              Text('عرض الكل', style: TextStyle(fontSize: 14, color: AppColors.gold, fontWeight: FontWeight.bold)),
            ],
          ),
        ),

        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: GridView.count(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisCount: crossAxisCount,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            childAspectRatio: childAspectRatio,
            children: [
              ServiceCard(title: 'الفنادق والمنتجعات', icon: Icons.hotel_outlined, isHighlighted: true, onTap: () => context.push('/hotels')),
              ServiceCard(title: 'المطاعم والكافيهات', icon: Icons.restaurant_menu_outlined, onTap: () => context.push('/restaurants')),
              ServiceCard(title: 'الرحلات السياحية', icon: Icons.landscape_outlined, onTap: () => context.push('/tours')),
              ServiceCard(title: 'خدمة التوصيل', icon: Icons.directions_car_filled_outlined, onTap: () => context.push('/transport')),
              ServiceCard(title: 'خطط رحلتك', icon: Icons.flight_land_outlined, onTap: () => context.push('/journey')),
            ],
          ),
        ),
        const SizedBox(height: 30),
      ],
    );
  }
}
