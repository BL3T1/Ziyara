import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../data/repositories/fake_restaurants_repository.dart';
import '../../../../core/widgets/shimmer/restaurant_card_shimmer.dart';
import '../widgets/restaurant_card.dart';
import 'restaurant_details_page.dart';
import '../bloc/restaurant_bloc.dart';
import '../bloc/restaurant_event.dart';
import '../bloc/restaurant_state.dart';

class RestaurantsPage extends StatelessWidget {
  const RestaurantsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => RestaurantBloc(
        repository: FakeRestaurantsRepository(),
      )..add(FetchRestaurants()),
      child: const RestaurantsView(),
    );
  }
}

class RestaurantsView extends StatefulWidget {
  const RestaurantsView({super.key});

  @override
  State<RestaurantsView> createState() => _RestaurantsViewState();
}

class _RestaurantsViewState extends State<RestaurantsView> {
  String _selectedCity = 'الكل';
  final List<String> _cities = ['الكل', 'دمشق', 'اللاذقية', 'حلب'];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('المطاعم والكافيهات'),
        backgroundColor: AppColors.background,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<RestaurantBloc>().add(FetchRestaurants()),
          )
        ],
      ),
      body: Column(
        children: [
          SizedBox(
            height: 50,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: _cities.length,
              itemBuilder: (context, index) {
                final city = _cities[index];
                final isSelected = _selectedCity == city;
                return Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: FilterChip(
                    label: Text(city),
                    selected: isSelected,
                    selectedColor: AppColors.primaryDark,
                    labelStyle: TextStyle(color: isSelected ? Colors.white : AppColors.textMain),
                    checkmarkColor: Colors.white,
                    onSelected: (selected) {
                      setState(() {
                        _selectedCity = city;
                      });
                    },
                  ),
                );
              },
            ),
          ),
          
          Expanded(
            child: BlocBuilder<RestaurantBloc, RestaurantState>(
              builder: (context, state) {
                if (state is RestaurantLoading || state is RestaurantInitial) {
                  return ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: 3,
                    itemBuilder: (context, index) => const RestaurantCardShimmer(),
                  );
                } else if (state is RestaurantError) {
                  return Center(child: Text(state.message));
                } else if (state is RestaurantLoaded) {
                  final filtered = _selectedCity == 'الكل' 
                    ? state.restaurants 
                    : state.restaurants.where((r) => r.location == _selectedCity).toList();

                  if (filtered.isEmpty) {
                    return const Center(child: Text('لا توجد مطاعم في هذه المدنية حالياً'));
                  }

                  return ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    itemBuilder: (context, index) {
                      final restaurant = filtered[index];
                      return RestaurantCard(
                        restaurant: restaurant,
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(builder: (_) => RestaurantDetailsPage(restaurant: restaurant)),
                          );
                        },
                      );
                    },
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          ),
        ],
      ),
    );
  }
}
