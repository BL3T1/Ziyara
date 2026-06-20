import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/di/injection_container.dart';
import '../bloc/auth_bloc.dart';
import '../bloc/auth_event.dart';
import '../bloc/auth_state.dart';

class SignupPage extends StatelessWidget {
  const SignupPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (_) => AuthBloc(repository: sl()),
      child: const _SignupView(),
    );
  }
}

class _SignupView extends StatefulWidget {
  const _SignupView();

  @override
  State<_SignupView> createState() => _SignupPageState();
}

class _SignupPageState extends State<_SignupView> {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _dobController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirm = true;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _dobController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthBloc>().add(SignUpRequested(
          email: _emailController.text.trim(),
          password: _passwordController.text,
          firstName: _firstNameController.text.trim(),
          lastName: _lastNameController.text.trim(),
          phone: _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
          dateOfBirth: _dobController.text.isEmpty ? null : _dobController.text,
        ));
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthBloc, AuthState>(
      listener: (context, state) {
        if (state is AuthSignedUp) {
          context.push('/otp_verify');
        } else if (state is AuthError) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(state.message), backgroundColor: Colors.red),
          );
        }
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('إنشاء حساب جديد')),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              children: [
                const Text('أدخل بياناتك للانضمام إلينا',
                    style: TextStyle(color: AppColors.textGrey, fontSize: 16)),
                const SizedBox(height: 30),
                _buildTextField(_firstNameController, 'الاسم الأول', Icons.person, true),
                const SizedBox(height: 16),
                _buildTextField(_lastNameController, 'اسم العائلة', Icons.person_outline, true),
                const SizedBox(height: 16),
                _buildTextField(_emailController, 'البريد الإلكتروني', Icons.email, true,
                    type: TextInputType.emailAddress),
                const SizedBox(height: 16),
                _buildTextField(_phoneController, 'رقم الهاتف', Icons.phone, false,
                    type: TextInputType.phone),
                const SizedBox(height: 16),
                _buildDatePickerField(),
                const SizedBox(height: 16),
                _buildPasswordField(_passwordController, 'كلمة المرور', _obscurePassword, () {
                  setState(() => _obscurePassword = !_obscurePassword);
                }),
                const SizedBox(height: 16),
                _buildPasswordField(
                    _confirmPasswordController, 'تأكيد كلمة المرور', _obscureConfirm, () {
                  setState(() => _obscureConfirm = !_obscureConfirm);
                }, isConfirm: true),
                const SizedBox(height: 40),
                BlocBuilder<AuthBloc, AuthState>(
                  builder: (context, state) {
                    final loading = state is AuthLoading;
                    return SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: loading ? null : _submit,
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          backgroundColor: AppColors.primaryDark,
                        ),
                        child: loading
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                    strokeWidth: 2, color: Colors.white),
                              )
                            : const Text('إرسال رمز التحقق OTP',
                                style: TextStyle(
                                    fontSize: 16, fontWeight: FontWeight.bold)),
                      ),
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildPasswordField(
    TextEditingController controller,
    String label,
    bool obscure,
    VoidCallback onToggle, {
    bool isConfirm = false,
  }) {
    return TextFormField(
      controller: controller,
      obscureText: obscure,
      decoration: InputDecoration(
        labelText: '$label (إجباري)',
        prefixIcon: const Icon(Icons.lock),
        filled: true,
        fillColor: Colors.white,
        suffixIcon: IconButton(
          icon: Icon(obscure ? Icons.visibility_off : Icons.visibility),
          onPressed: onToggle,
        ),
      ),
      validator: (v) {
        if (v == null || v.isEmpty) return 'مطلوب';
        if (!isConfirm && v.length < 8) return 'كلمة المرور يجب أن تكون 8 أحرف على الأقل';
        if (isConfirm && v != _passwordController.text) return 'كلمتا المرور غير متطابقتين';
        return null;
      },
    );
  }

  Widget _buildTextField(
    TextEditingController controller,
    String label,
    IconData icon,
    bool isRequired, {
    TextInputType type = TextInputType.text,
  }) {
    return TextFormField(
      controller: controller,
      keyboardType: type,
      decoration: InputDecoration(
        labelText: isRequired ? '$label (إجباري)' : '$label (اختياري)',
        prefixIcon: Icon(icon),
        filled: true,
        fillColor: Colors.white,
      ),
      validator: isRequired ? (v) => (v == null || v.isEmpty) ? 'مطلوب' : null : null,
    );
  }

  Widget _buildDatePickerField() {
    return TextFormField(
      controller: _dobController,
      readOnly: true,
      decoration: const InputDecoration(
        labelText: 'تاريخ الميلاد (اختياري)',
        prefixIcon: Icon(Icons.calendar_today),
        filled: true,
        fillColor: Colors.white,
      ),
      onTap: () async {
        final picked = await showDatePicker(
          context: context,
          initialDate: DateTime(2000),
          firstDate: DateTime(1900),
          lastDate: DateTime.now(),
        );
        if (picked != null) {
          _dobController.text =
              '${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
        }
      },
    );
  }
}
