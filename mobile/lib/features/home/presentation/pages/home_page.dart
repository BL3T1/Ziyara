import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../widgets/home_body.dart';
import '../../../bookings/presentation/pages/my_bookings_page.dart';
import '../../../bookings/presentation/pages/favorites_page.dart';
import '../../../profile/presentation/pages/profile_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() =>
      _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _bottomNavIndex = 0;
  final List<Widget> _pages = [
    const HomeBody(),
    const MyBookingsPage(),
    const FavoritesPage(),
    const ProfilePage(),
  ];

  @override
  Widget build(BuildContext context) {
    final isDark =
        Theme.of(context).brightness ==
        Brightness.dark;

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            if (_bottomNavIndex == 0)
              Padding(
                padding:
                    const EdgeInsets.symmetric(
                      horizontal: 20.0,
                      vertical: 16.0,
                    ),
                child: Row(
                  mainAxisAlignment:
                      MainAxisAlignment
                          .spaceBetween,
                  crossAxisAlignment:
                      CrossAxisAlignment.center,
                  children: [
                    Column(
                      crossAxisAlignment:
                          CrossAxisAlignment
                              .start,
                      children: [
                        Text(
                          'ZIYARA',
                          style: TextStyle(
                            fontFamily: 'Serif',
                            fontSize: 28,
                            fontWeight:
                                FontWeight.bold,
                            color: isDark
                                ? Colors.white
                                : AppColors
                                      .primaryBlue,
                            letterSpacing: 2.0,
                          ),
                        ),
                        const SizedBox(height: 2),
                        // --- التعديل هنا: زيادة العرض من 40 إلى 100 ---
                        Container(
                          height: 3,
                          width: 120,
                          color: AppColors.cyan,
                        ),
                      ],
                    ),

                    Stack(
                      children: [
                        Container(
                          decoration: BoxDecoration(
                            color: isDark
                                ? Colors.white
                                      .withOpacity(
                                        0.1,
                                      )
                                : Colors.white,
                            shape:
                                BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: Colors.grey
                                    .withOpacity(
                                      0.1,
                                    ),
                                blurRadius: 10,
                                spreadRadius: 2,
                              ),
                            ],
                          ),
                          child: IconButton(
                            icon: Icon(
                              Icons
                                  .notifications_none,
                              color: isDark
                                  ? Colors.white
                                  : AppColors
                                        .primaryDark,
                              size: 28,
                            ),
                            onPressed: () =>
                                context.push(
                                  '/notifications',
                                ),
                          ),
                        ),
                        Positioned(
                          top: 10,
                          right: 12,
                          child: Container(
                            padding:
                                const EdgeInsets.all(
                                  4,
                                ),
                            decoration:
                                const BoxDecoration(
                                  color:
                                      Colors.red,
                                  shape: BoxShape
                                      .circle,
                                ),
                            child: const Text(
                              '3',
                              style: TextStyle(
                                color:
                                    Colors.white,
                                fontSize: 10,
                                fontWeight:
                                    FontWeight
                                        .bold,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            Expanded(
              child: IndexedStack(
                index: _bottomNavIndex,
                children: _pages,
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomNav(
        isDark,
      ),
    );
  }

  Widget _buildBottomNav(bool isDark) {
    return Container(
      decoration: const BoxDecoration(
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 10,
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(20),
        ),
        child: BottomNavigationBar(
          currentIndex: _bottomNavIndex,
          onTap: (index) => setState(
            () => _bottomNavIndex = index,
          ),
          type: BottomNavigationBarType.fixed,
          backgroundColor: isDark
              ? const Color(0xFF1E1E1E)
              : AppColors.primaryDark,
          selectedItemColor: AppColors.cyan,
          unselectedItemColor: Colors.white70,
          showSelectedLabels: true,
          showUnselectedLabels: true,
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.home_filled),
              label: 'الرئيسية',
            ),
            BottomNavigationBarItem(
              icon: Icon(
                Icons
                    .confirmation_number_outlined,
              ),
              label: 'حجوزاتي',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.favorite_border),
              label: 'المفضلة',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.settings_outlined),
              label: 'الإعدادات',
            ),
          ],
        ),
      ),
    );
  }
}
