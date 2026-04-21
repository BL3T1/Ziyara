import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/launcher_helper.dart';
import '../../../../core/api/api_client.dart';
import '../widgets/payment_summary_card.dart';
import '../bloc/payment_bloc.dart';
import '../bloc/payment_event.dart';
import '../bloc/payment_state.dart';
import '../../data/repositories/payment_repository_impl.dart';

class PaymentPage extends StatelessWidget {
  final String title;
  final double price;
  final String imageUrl;

  const PaymentPage({super.key, required this.title, required this.price, required this.imageUrl});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => PaymentBloc(
        repository: PaymentRepositoryImpl(apiClient: ApiClient()),
        basePrice: price,
      ),
      child: PaymentView(title: title, basePrice: price, imageUrl: imageUrl),
    );
  }
}

class PaymentView extends StatefulWidget {
  final String title;
  final double basePrice;
  final String imageUrl;

  const PaymentView({super.key, required this.title, required this.basePrice, required this.imageUrl});

  @override
  State<PaymentView> createState() => _PaymentViewState();
}

class _PaymentViewState extends State<PaymentView> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _couponController = TextEditingController();
  File? _idImage;

  Future<void> _pickIdImage() async {
    final ImagePicker picker = ImagePicker();
    showModalBottomSheet(
      context: context,
      builder: (ctx) => Wrap(children: [
        ListTile(leading: const Icon(Icons.camera_alt), title: const Text('التقاط بالكاميرا'), onTap: () async { Navigator.pop(ctx); _setImage(await picker.pickImage(source: ImageSource.camera)); }),
        ListTile(leading: const Icon(Icons.photo_library), title: const Text('اختيار من المعرض'), onTap: () async { Navigator.pop(ctx); _setImage(await picker.pickImage(source: ImageSource.gallery)); }),
      ]),
    );
  }

  void _setImage(XFile? image) { if (image != null) setState(() => _idImage = File(image.path)); }

  @override
  Widget build(BuildContext context) {
    return BlocListener<PaymentBloc, PaymentState>(
      listener: (context, state) {
        if (state is PaymentSuccess) {
          context.push('/payment_success');
        } else if (state is PaymentError) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.message), backgroundColor: AppColors.error));
        }
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('تأكيد الحجز'), backgroundColor: AppColors.background),
        body: Form(
          key: _formKey,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: BlocBuilder<PaymentBloc, PaymentState>(
              builder: (context, state) {
                double totalPrice = (widget.basePrice * state.personCount) - state.discountAmount;

                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    PaymentSummaryCard(title: widget.title, price: widget.basePrice, imageUrl: widget.imageUrl),
                    const SizedBox(height: 24),
                    
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.grey.shade200)),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('عدد الأشخاص', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                          Row(
                            children: [
                              IconButton(onPressed: () { if (state.personCount > 1) context.read<PaymentBloc>().add(UpdatePersonCount(state.personCount - 1)); }, icon: const Icon(Icons.remove_circle_outline)),
                              Text('${state.personCount}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                              IconButton(onPressed: () { context.read<PaymentBloc>().add(UpdatePersonCount(state.personCount + 1)); }, icon: const Icon(Icons.add_circle_outline, color: AppColors.cyan)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    
                    const SizedBox(height: 24),
                    const Text('بيانات الحاجز', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 10),
                    TextFormField(
                      controller: _nameController,
                      decoration: const InputDecoration(labelText: 'الاسم الرباعي', prefixIcon: Icon(Icons.person), filled: true, fillColor: Colors.white),
                      validator: (v) => v!.isEmpty ? 'مطلوب' : null,
                    ),
                    
                    const SizedBox(height: 16),
                    const Text('إثبات الشخصية (إجباري)', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    InkWell(
                      onTap: _pickIdImage,
                      child: Container(
                        height: 120,
                        width: double.infinity,
                        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: _idImage == null ? Colors.grey.shade300 : AppColors.success, width: 2)),
                        child: _idImage == null 
                          ? const Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.add_a_photo, color: AppColors.textGrey), Text('اضغط لرفع صورة الهوية/الجواز')])
                          : ClipRRect(borderRadius: BorderRadius.circular(10), child: Image.file(_idImage!, fit: BoxFit.cover)),
                      ),
                    ),
                    if (_idImage == null)
                       const Padding(padding: EdgeInsets.only(top: 5), child: Text('* يجب رفع صورة الهوية لإتمام الحجز', style: TextStyle(color: AppColors.error, fontSize: 12))),
                    
                    const SizedBox(height: 24),
                    Row(
                      children: [
                        Expanded(child: TextField(controller: _couponController, enabled: !state.isCouponApplied, decoration: const InputDecoration(hintText: 'كود الخصم', filled: true, fillColor: Colors.white))),
                        const SizedBox(width: 10),
                        ElevatedButton(
                          onPressed: state.isCouponApplied ? null : () {
                            context.read<PaymentBloc>().add(ApplyCoupon(_couponController.text));
                          },
                          child: const Text('تطبيق'),
                        ),
                      ],
                    ),
                    
                    const SizedBox(height: 24),
                    const Divider(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('الإجمالي', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                        Text('${totalPrice.toInt()} ل.س', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppColors.primaryBlue)),
                      ],
                    ),
                    
                    const SizedBox(height: 30),
                    Center(child: TextButton.icon(
                      onPressed: () => LauncherHelper.openWhatsApp(context, '+963900000000'), 
                      icon: const Icon(Icons.support_agent, color: AppColors.gold), 
                      label: const Text('بحاجة مساعدة؟ تواصل مع الدعم', style: TextStyle(color: AppColors.gold)),
                    )),
                    
                    const SizedBox(height: 10),
                    if (state is PaymentProcessing)
                      const Center(child: CircularProgressIndicator())
                    else
                      SizedBox(width: double.infinity, child: ElevatedButton(
                        onPressed: () {
                          if (_formKey.currentState!.validate()) {
                            if (_idImage != null) {
                              context.read<PaymentBloc>().add(ConfirmPayment(
                                amount: totalPrice,
                                details: {'name': _nameController.text},
                                idImage: _idImage!,
                              ));
                            } else {
                              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('عذراً، صورة الهوية مطلوبة'), backgroundColor: AppColors.error));
                            }
                          }
                        },
                        style: ElevatedButton.styleFrom(backgroundColor: AppColors.primaryDark, padding: const EdgeInsets.symmetric(vertical: 16)),
                        child: const Text('تأكيد الدفع', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      )),
                  ],
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}
