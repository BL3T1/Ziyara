import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../data/repositories/fake_hotels_repository.dart';
import '../../../../core/widgets/shimmer/hotel_card_shimmer.dart';
import '../../../../core/services/search_history_service.dart';
import '../widgets/hotel_card.dart';
import 'hotel_details_page.dart';
import '../bloc/hotel_bloc.dart';
import '../bloc/hotel_event.dart';
import '../bloc/hotel_state.dart';

class HotelsPage extends StatelessWidget {
  const HotelsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => HotelBloc(repository: FakeHotelsRepository())..add(FetchHotels()),
      child: const HotelsView(),
    );
  }
}

class HotelsView extends StatefulWidget {
  const HotelsView({super.key});

  @override
  State<HotelsView> createState() => _HotelsViewState();
}

class _HotelsViewState extends State<HotelsView> {
  final List<String> _cities = ['الكل', 'دمشق', 'اللاذقية', 'طرطوس', 'حلب', 'حمص'];
  final TextEditingController _searchController = TextEditingController();
  bool _showHistory = false;

  @override
  void initState() {
    super.initState();
    _searchController.addListener(() {
      if (_searchController.text.isEmpty) {
        setState(() => _showHistory = true);
      } else {
        setState(() => _showHistory = false);
      }
    });
  }

  void _onSearchChanged(BuildContext context, String selectedCity) {
    context.read<HotelBloc>().add(FilterHotels(
      query: _searchController.text,
      city: selectedCity,
    ));
    if (_searchController.text.isNotEmpty) {
      SearchHistoryService.addSearchTerm(_searchController.text);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('الفنادق والمنتجعات'),
        backgroundColor: AppColors.background,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<HotelBloc>().add(FetchHotels()),
          )
        ],
      ),
      body: BlocBuilder<HotelBloc, HotelState>(
        builder: (context, state) {
          String currentCity = 'الكل';
          if (state is HotelLoaded) {
            currentCity = state.selectedCity;
          }

          return Column(
            children: [
              // شريط البحث
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Focus(
                  onFocusChange: (hasFocus) {
                    setState(() => _showHistory = hasFocus && _searchController.text.isEmpty);
                  },
                  child: TextField(
                    controller: _searchController,
                    onChanged: (_) => _onSearchChanged(context, currentCity),
                    onSubmitted: (val) {
                      SearchHistoryService.addSearchTerm(val);
                      _onSearchChanged(context, currentCity);
                    },
                    decoration: InputDecoration(
                      hintText: 'ابحث عن فندق...',
                      prefixIcon: const Icon(Icons.search),
                      fillColor: Colors.white,
                      filled: true,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                      suffixIcon: _searchController.text.isNotEmpty 
                        ? IconButton(
                            icon: const Icon(Icons.clear), 
                            onPressed: () {
                              _searchController.clear();
                              _onSearchChanged(context, currentCity);
                              setState(() => _showHistory = true);
                            }
                          ) 
                        : null,
                    ),
                  ),
                ),
              ),

              // --- قسم سجل البحث ---
              ValueListenableBuilder<List<String>>(
                valueListenable: SearchHistoryService.historyNotifier,
                builder: (context, history, child) {
                  if (!_showHistory || history.isEmpty) return const SizedBox.shrink();
                  
                  return Container(
                    margin: const EdgeInsets.symmetric(horizontal: 16),
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text('عمليات البحث الأخيرة', style: TextStyle(fontSize: 12, color: AppColors.textGrey, fontWeight: FontWeight.bold)),
                            InkWell(
                              onTap: () => SearchHistoryService.clearHistory(),
                              child: const Text('مسح', style: TextStyle(fontSize: 12, color: AppColors.error)),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: history.map((term) => ActionChip(
                            label: Text(term),
                            backgroundColor: AppColors.background,
                            onPressed: () {
                              _searchController.text = term;
                              _onSearchChanged(context, currentCity);
                              setState(() => _showHistory = false);
                              FocusScope.of(context).unfocus();
                            },
                          )).toList(),
                        ),
                      ],
                    ),
                  );
                },
              ),

              // فلتر المحافظات
              SizedBox(
                height: 50,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: _cities.length,
                  itemBuilder: (context, index) {
                    final city = _cities[index];
                    final isSelected = currentCity == city;
                    return Padding(
                      padding: const EdgeInsets.only(left: 8),
                      child: ChoiceChip(
                        label: Text(city),
                        selected: isSelected,
                        selectedColor: AppColors.primaryDark,
                        labelStyle: TextStyle(
                          color: isSelected ? Colors.white : AppColors.textMain,
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        ),
                        backgroundColor: Colors.white,
                        onSelected: (selected) {
                          if (selected) {
                            _onSearchChanged(context, city);
                          }
                        },
                      ),
                    );
                  },
                ),
              ),
              
              const SizedBox(height: 10),

              // القائمة
              Expanded(
                child: _buildList(state),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildList(HotelState state) {
    if (state is HotelLoading || state is HotelInitial) {
      return ListView.builder(padding: const EdgeInsets.all(16), itemCount: 4, itemBuilder: (context, index) => const HotelCardShimmer());
    } else if (state is HotelError) {
      return Center(child: Text(state.message));
    } else if (state is HotelLoaded) {
      if (state.filteredHotels.isEmpty) {
        return const Center(child: Text('لا توجد نتائج مطابقة للبحث'));
      }
      return ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: state.filteredHotels.length,
        itemBuilder: (context, index) {
          final hotel = state.filteredHotels[index];
          return HotelCard(
            hotel: hotel,
            onTap: () {
              Navigator.push(context, MaterialPageRoute(builder: (context) => HotelDetailsPage(hotel: hotel)));
            },
          );
        },
      );
    }
    return const SizedBox.shrink();
  }
}
