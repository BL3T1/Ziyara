import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';

class SignupPage extends StatefulWidget {
  const SignupPage({super.key});

  @override
  State<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends State<SignupPage> {
  final _formKey = GlobalKey<FormState>();
  // استخدام Controllers للتحكم بالبيانات
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _dobController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('إنشاء حساب جديد')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              const Text('أدخل بياناتك للانضمام إلينا', style: TextStyle(color: AppColors.textGrey, fontSize: 16)),
              const SizedBox(height: 30),
              
              // الحقول
              _buildTextField(_nameController, 'الاسم الكامل', Icons.person, true),
              const SizedBox(height: 16),
              _buildTextField(_emailController, 'البريد الإلكتروني', Icons.email, true, type: TextInputType.emailAddress),
              const SizedBox(height: 16),
              _buildTextField(_phoneController, 'رقم الهاتف', Icons.phone, false, type: TextInputType.phone),
              const SizedBox(height: 16),
              _buildDatePickerField(), // حقل التاريخ المنفصل
              
              const SizedBox(height: 40),
              
              // زر الإرسال
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    if (_formKey.currentState!.validate()) {
                      context.push('/otp_verify');
                    }
                  },
                  style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16), backgroundColor: AppColors.primaryDark),
                  child: const Text('إرسال رمز التحقق OTP', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // دالة مساعدة لبناء الحقول النصية لتقليل التكرار
  Widget _buildTextField(TextEditingController controller, String label, IconData icon, bool isRequired, {TextInputType type = TextInputType.text}) {
    return TextFormField(
      controller: controller,
      keyboardType: type,
      decoration: InputDecoration(
        labelText: isRequired ? '$label (إجباري)' : '$label (اختياري)',
        prefixIcon: Icon(icon),
        filled: true, fillColor: Colors.white,
      ),
      validator: isRequired ? (v) => v!.isEmpty ? 'مطلوب' : null : null,
    );
  }

  // ويدجت اختيار التاريخ
  Widget _buildDatePickerField() {
    return TextFormField(
      controller: _dobController,
      readOnly: true,
      decoration: const InputDecoration(labelText: 'تاريخ الميلاد', prefixIcon: Icon(Icons.calendar_today), filled: true, fillColor: Colors.white),
      onTap: () async {
        DateTime? picked = await showDatePicker(
          context: context,
          initialDate: DateTime(2000),
          firstDate: DateTime(1900),
          lastDate: DateTime.now(),
        );
        if (picked != null) {
          _dobController.text = "${picked.year}-${picked.month}-${picked.day}";
        }
      },
    );
  }
}
