import 'package:carousel_slider/carousel_slider.dart';
import 'package:flutter/material.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/dummy_image_manager.dart'; // استيراد الملف الجديد

class HomeBannerSlider extends StatefulWidget {
  const HomeBannerSlider({super.key});

  @override
  State<HomeBannerSlider> createState() => _HomeBannerSliderState();
}

class _HomeBannerSliderState extends State<HomeBannerSlider> {
  int _currentIndex = 0;
  
  final List<Map<String, String>> _banners = [
    {
      'image': DummyImageManager.bannerDamascus, // استخدام المتغير
      'title': 'سحر دمشق القديمة',
      'subtitle': 'عبق التاريخ في أقدم عاصمة مأهولة'
    },
    {
      'image': DummyImageManager.bannerResort,
      'title': 'رفاهية لا تنتهي',
      'subtitle': 'أفضل المنتجعات في الساحل السوري'
    },
    {
      'image': DummyImageManager.bannerCastle,
      'title': 'قلاع وحصون',
      'subtitle': 'اكتشف عظمة العمارة التاريخية'
    },
  ];

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        CarouselSlider(
          options: CarouselOptions(
            height: 200.0,
            autoPlay: true,
            enlargeCenterPage: true,
            aspectRatio: 16 / 9,
            viewportFraction: 0.90,
            onPageChanged: (index, reason) => setState(() => _currentIndex = index),
          ),
          items: _banners.map((banner) {
            return Builder(
              builder: (BuildContext context) {
                return Container(
                  width: MediaQuery.of(context).size.width,
                  margin: const EdgeInsets.symmetric(horizontal: 5.0),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: Stack(
                      children: [
                        CachedNetworkImage(
                          imageUrl: banner['image']!,
                          fit: BoxFit.cover,
                          width: double.infinity,
                          height: double.infinity,
                          placeholder: (context, url) => Container(color: Colors.grey[300]),
                          errorWidget: (context, url, error) => const Icon(Icons.error),
                        ),
                        Container(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              begin: Alignment.bottomCenter,
                              end: Alignment.topCenter,
                              colors: [Colors.black.withOpacity(0.8), Colors.transparent],
                            ),
                          ),
                        ),
                        Positioned(
                          bottom: 16, right: 16, left: 16,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(banner['title']!, style: const TextStyle(color: AppColors.gold, fontSize: 18, fontWeight: FontWeight.bold)),
                              Text(banner['subtitle']!, style: const TextStyle(color: Colors.white, fontSize: 12)),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            );
          }).toList(),
        ),
        const SizedBox(height: 12),
        AnimatedSmoothIndicator(
          activeIndex: _currentIndex,
          count: _banners.length,
          effect: const ExpandingDotsEffect(activeDotColor: AppColors.cyan, dotColor: Color(0xFFD1D5DB), dotHeight: 6, dotWidth: 6),
        ),
      ],
    );
  }
}
