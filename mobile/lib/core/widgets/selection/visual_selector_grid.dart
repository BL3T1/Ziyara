import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';

class VisualSelectorGrid extends StatefulWidget {
  final int totalItems;
  final List<int> bookedItems;
  final String itemLabel;
  final Function(int) onSelected;

  const VisualSelectorGrid({super.key, required this.totalItems, required this.bookedItems, required this.itemLabel, required this.onSelected});

  @override
  State<VisualSelectorGrid> createState() => _VisualSelectorGridState();
}

class _VisualSelectorGridState extends State<VisualSelectorGrid> {
  int? _selectedItem;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildLegendItem(AppColors.success, 'متاح'),
            const SizedBox(width: 15),
            _buildLegendItem(AppColors.error, 'محجوز'),
            const SizedBox(width: 15),
            _buildLegendItem(AppColors.cyan, 'اختيارك'), // اللون السماوي
          ],
        ),
        const SizedBox(height: 16),
        
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 5, crossAxisSpacing: 10, mainAxisSpacing: 10),
          itemCount: widget.totalItems,
          itemBuilder: (context, index) {
            final itemNumber = index + 1;
            final isBooked = widget.bookedItems.contains(itemNumber);
            final isSelected = _selectedItem == itemNumber;

            return GestureDetector(
              onTap: isBooked ? null : () { setState(() => _selectedItem = itemNumber); widget.onSelected(itemNumber); },
              child: Container(
                decoration: BoxDecoration(
                  color: isSelected ? AppColors.cyan : Colors.white, // التحديد سماوي، العادي أبيض
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: isBooked ? Colors.grey[300]! : (isSelected ? AppColors.cyan : Colors.grey.shade300), width: 2),
                ),
                child: Stack(
                  children: [
                    Center(child: Text('$itemNumber', style: TextStyle(color: isSelected ? Colors.white : Colors.black, fontWeight: FontWeight.bold))),
                    // النقطة الصغيرة في الزاوية
                    Positioned(
                      top: 4, right: 4,
                      child: Container(
                        width: 8, height: 8,
                        decoration: BoxDecoration(
                          color: isBooked ? AppColors.error : AppColors.success, // أحمر للمحجوز، أخضر للمتاح
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 10),
        if (_selectedItem != null)
          Text('تم اختيار ${widget.itemLabel} رقم $_selectedItem', style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.cyan)),
      ],
    );
  }

  Widget _buildLegendItem(Color color, String label) {
    return Row(children: [Container(width: 12, height: 12, decoration: BoxDecoration(color: color, shape: BoxShape.circle)), const SizedBox(width: 5), Text(label, style: const TextStyle(fontSize: 12))]);
  }
}
