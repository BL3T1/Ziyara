import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/api/api_client.dart';
import '../bloc/profile_bloc.dart';
import '../bloc/profile_event.dart';
import '../bloc/profile_state.dart';
import '../../data/repositories/profile_repository_impl.dart';

class VerificationPage extends StatelessWidget {
  const VerificationPage({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider(
      create: (context) => ProfileBloc(
        repository: ProfileRepositoryImpl(apiClient: ApiClient()),
      ),
      child: const VerificationView(),
    );
  }
}

class VerificationView extends StatefulWidget {
  const VerificationView({super.key});

  @override
  State<VerificationView> createState() => _VerificationViewState();
}

class _VerificationViewState extends State<VerificationView> {
  File? _idFront;
  File? _idBack;

  Future<void> _pickImage(bool isFront) async {
    final ImagePicker picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        if (isFront) _idFront = File(image.path);
        else _idBack = File(image.path);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<ProfileBloc, ProfileState>(
      listener: (context, state) {
        if (state is VerificationSubmitSuccess) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم إرسال المستندات بنجاح، سيتم التدقيق قريباً'), backgroundColor: AppColors.success));
          Navigator.pop(context);
        } else if (state is ProfileError) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(state.message), backgroundColor: AppColors.error));
        }
      },
      child: Scaffold(
        appBar: AppBar(title: const Text('توثيق الحساب'), backgroundColor: AppColors.background),
        body: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            children: [
              const Icon(Icons.verified_user_outlined, size: 80, color: AppColors.gold),
              const SizedBox(height: 20),
              const Text(
                'قم برفع صورة الهوية أو جواز السفر لتفعيل كافة ميزات التطبيق',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 16, color: AppColors.textGrey),
              ),
              const SizedBox(height: 40),
              _buildUploadOption(Icons.badge_outlined, _idFront == null ? 'رفع وجه الهوية الأمامي' : 'تم اختيار الوجه الأمامي', () => _pickImage(true), _idFront != null),
              const SizedBox(height: 16),
              _buildUploadOption(Icons.badge_outlined, _idBack == null ? 'رفع وجه الهوية الخلفي' : 'تم اختيار الوجه الخلفي', () => _pickImage(false), _idBack != null),
              const Spacer(),
              BlocBuilder<ProfileBloc, ProfileState>(
                builder: (context, state) {
                  return ElevatedButton(
                    onPressed: (_idFront == null || _idBack == null || state is ProfileLoading) ? null : () {
                      context.read<ProfileBloc>().add(SubmitAccountVerification(idFront: _idFront!, idBack: _idBack!));
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primaryDark,
                      minimumSize: const Size(double.infinity, 50),
                    ),
                    child: state is ProfileLoading ? const CircularProgressIndicator(color: Colors.white) : const Text('إرسال للتحقق'),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildUploadOption(IconData icon, String title, VoidCallback onTap, bool isSelected) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: isSelected ? AppColors.success : AppColors.primaryBlue.withOpacity(0.2), width: 1),
        ),
        child: Row(
          children: [
            Icon(icon, color: isSelected ? AppColors.success : AppColors.primaryBlue, size: 28),
            const SizedBox(width: 16),
            Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: isSelected ? AppColors.success : Colors.black)),
            const Spacer(),
            Icon(isSelected ? Icons.check_circle : Icons.arrow_forward_ios, size: 16, color: isSelected ? AppColors.success : AppColors.textGrey),
          ],
        ),
      ),
    );
  }
}
