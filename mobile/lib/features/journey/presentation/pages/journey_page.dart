import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/di/injection_container.dart';
import '../../domain/repositories/journey_repository.dart';
import '../../data/models/journey_item_model.dart';
import '../bloc/journey_bloc.dart';
import '../bloc/journey_event.dart';
import '../bloc/journey_state.dart';

class JourneyPage extends StatelessWidget {
  const JourneyPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => JourneyBloc(repository: sl<JourneyRepository>()),
      child: const _JourneyView(),
    );
  }
}

class _JourneyView extends StatefulWidget {
  const _JourneyView();

  @override
  State<_JourneyView> createState() => _JourneyViewState();
}

class _JourneyViewState extends State<_JourneyView> {
  final _cityCtrl = TextEditingController();
  final _airportCtrl = TextEditingController();
  final _budgetCtrl = TextEditingController();
  int _guests = 1;

  @override
  void dispose() {
    _cityCtrl.dispose();
    _airportCtrl.dispose();
    _budgetCtrl.dispose();
    super.dispose();
  }

  void _submit() {
    final city = _cityCtrl.text.trim();
    if (city.isEmpty) return;
    final budget = double.tryParse(_budgetCtrl.text.trim());
    context.read<JourneyBloc>().add(FetchJourneyRecommendations(
          city: city,
          guests: _guests,
          maxBudget: budget,
        ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('خطط رحلتك', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: AppColors.primaryDark,
        foregroundColor: Colors.white,
      ),
      body: BlocBuilder<JourneyBloc, JourneyState>(
        builder: (context, state) {
          if (state is JourneyLoading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state is JourneyLoaded) {
            return _ResultsView(
              data: state.data,
              onReset: () => context.read<JourneyBloc>().emit(JourneyInitial()),
            );
          }
          return _FormView(
            cityCtrl: _cityCtrl,
            airportCtrl: _airportCtrl,
            budgetCtrl: _budgetCtrl,
            guests: _guests,
            onGuestsChanged: (v) => setState(() => _guests = v),
            onSubmit: _submit,
            error: state is JourneyError ? (state as JourneyError).message : null,
          );
        },
      ),
    );
  }
}

class _FormView extends StatelessWidget {
  final TextEditingController cityCtrl;
  final TextEditingController airportCtrl;
  final TextEditingController budgetCtrl;
  final int guests;
  final ValueChanged<int> onGuestsChanged;
  final VoidCallback onSubmit;
  final String? error;

  const _FormView({
    required this.cityCtrl,
    required this.airportCtrl,
    required this.budgetCtrl,
    required this.guests,
    required this.onGuestsChanged,
    required this.onSubmit,
    this.error,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('رحلتك كاملة في خطوة واحدة',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primaryDark)),
          const SizedBox(height: 6),
          const Text('أدخل تفاصيل وصولك واحصل على توصيات فندق وتاكسي ومطعم.',
              style: TextStyle(fontSize: 14, color: AppColors.textGrey)),
          const SizedBox(height: 24),
          _label('مطار الوصول'),
          _field(airportCtrl, 'مثال: مطار دمشق الدولي'),
          const SizedBox(height: 16),
          _label('مدينة الوجهة *'),
          _field(cityCtrl, 'مثال: دمشق'),
          const SizedBox(height: 16),
          _label('عدد الضيوف'),
          Row(
            children: [
              IconButton(
                onPressed: guests > 1 ? () => onGuestsChanged(guests - 1) : null,
                icon: const Icon(Icons.remove_circle_outline),
                color: AppColors.primaryDark,
              ),
              Text('$guests', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              IconButton(
                onPressed: () => onGuestsChanged(guests + 1),
                icon: const Icon(Icons.add_circle_outline),
                color: AppColors.primaryDark,
              ),
            ],
          ),
          const SizedBox(height: 16),
          _label('الحد الأقصى للميزانية لليلة (اختياري)'),
          _field(budgetCtrl, 'مثال: 150', inputType: TextInputType.number),
          if (error != null) ...[
            const SizedBox(height: 12),
            Text(error!, style: const TextStyle(color: AppColors.error, fontSize: 13)),
          ],
          const SizedBox(height: 28),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: onSubmit,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primaryDark,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              child: const Text('ابحث عن توصيات', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Text(text, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: AppColors.textDark)),
      );

  Widget _field(TextEditingController ctrl, String hint, {TextInputType? inputType}) => TextField(
        controller: ctrl,
        keyboardType: inputType,
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: const TextStyle(color: AppColors.textGrey),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: AppColors.primaryDark, width: 2),
          ),
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        ),
      );
}

class _ResultsView extends StatelessWidget {
  final JourneyRecommendationModel data;
  final VoidCallback onReset;

  const _ResultsView({required this.data, required this.onReset});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextButton.icon(
            onPressed: onReset,
            icon: const Icon(Icons.arrow_back, size: 16),
            label: const Text('تغيير التفاصيل'),
            style: TextButton.styleFrom(foregroundColor: AppColors.primaryDark),
          ),
          const SizedBox(height: 8),
          _Section(title: 'الفنادق والمنتجعات', items: data.hotels),
          const SizedBox(height: 24),
          _Section(title: 'التاكسي', items: data.taxis),
          const SizedBox(height: 24),
          _Section(title: 'المطاعم', items: data.restaurants),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  final String title;
  final List<JourneyItemModel> items;

  const _Section({required this.title, required this.items});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: AppColors.primaryDark)),
        const SizedBox(height: 10),
        if (items.isEmpty)
          const Text('لا توجد نتائج.', style: TextStyle(color: AppColors.textGrey))
        else
          ...items.map((item) => _ItemCard(item: item)),
      ],
    );
  }
}

class _ItemCard extends StatelessWidget {
  final JourneyItemModel item;

  const _ItemCard({required this.item});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: item.imageUrl.isNotEmpty
                  ? Image.network(item.imageUrl, width: 72, height: 72, fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => _placeholder())
                  : _placeholder(),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  if (item.city.isNotEmpty)
                    Text(item.city, style: const TextStyle(color: AppColors.textGrey, fontSize: 12)),
                  if (item.basePrice != null)
                    Text('من ${item.basePrice!.toStringAsFixed(0)} ${item.currency}',
                        style: const TextStyle(color: AppColors.gold, fontWeight: FontWeight.w600, fontSize: 13)),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward_ios, size: 14, color: AppColors.textGrey),
          ],
        ),
      ),
    );
  }

  Widget _placeholder() => Container(
        width: 72, height: 72,
        decoration: BoxDecoration(color: Colors.grey[200], borderRadius: BorderRadius.circular(10)),
        child: const Icon(Icons.image_outlined, color: Colors.grey),
      );
}
