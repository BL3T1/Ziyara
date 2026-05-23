import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/utils/launcher_helper.dart';
import '../../../../core/utils/app_settings.dart';
import '../../../../core/utils/biometric_helper.dart';
import '../../../../core/di/injection_container.dart';
import '../widgets/profile_header.dart';
import '../bloc/profile_bloc.dart';
import '../bloc/profile_event.dart';
import '../bloc/profile_state.dart';
import '../../../auth/presentation/bloc/auth_bloc.dart';
import '../../../auth/presentation/bloc/auth_event.dart';
import '../../../auth/presentation/bloc/auth_state.dart';
import '../../../auth/domain/repositories/auth_repository.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (context) => ProfileBloc(repository: sl())..add(FetchProfile()),
        ),
        BlocProvider(
          create: (context) => AuthBloc(repository: sl<AuthRepository>()),
        ),
      ],
      child: const ProfileView(),
    );
  }
}

class ProfileView extends StatelessWidget {
  const ProfileView({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocListener<AuthBloc, AuthState>(
      listener: (context, state) {
        if (state is AuthUnauthenticated) {
          context.go('/login');
        }
      },
      child: ValueListenableBuilder<Locale>(
        valueListenable: AppSettings.localeNotifier,
        builder: (context, locale, child) {
          bool isAr = locale.languageCode == 'ar';
        return Scaffold(
          appBar: AppBar(
            title: Text(isAr ? 'الإعدادات والملف الشخصي' : 'Settings'),
            backgroundColor: AppColors.primaryBlue,
          ),
          body: BlocBuilder<ProfileBloc, ProfileState>(
            builder: (context, state) {
              if (state is ProfileLoading) {
                return const Center(child: CircularProgressIndicator());
              }
              if (state is ProfileError) {
                return Center(child: Text(state.message));
              }
              if (state is ProfileLoaded) {
                final profile = state.profile;
                return SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: Column(
                    children: [
                      ProfileHeader(isAr: isAr, profile: profile),
                      const SizedBox(height: 20),
                      const Text('تواصل معنا', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.textGrey)),
                      const SizedBox(height: 10),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          _socialIcon(FontAwesomeIcons.whatsapp, Colors.green, 'whatsapp://send?phone=00963900000000', context),
                          const SizedBox(width: 20),
                          _socialIcon(FontAwesomeIcons.instagram, Colors.purple, 'https://instagram.com', context),
                          const SizedBox(width: 20),
                          _socialIcon(FontAwesomeIcons.facebook, Colors.blue, 'https://facebook.com', context),
                        ],
                      ),
                      const SizedBox(height: 20),
                      const Divider(),
                      _buildSectionTitle(isAr ? 'الحساب' : 'Account'),
                      _buildProfileTile(Icons.person_outline, isAr ? 'تعديل البيانات' : 'Edit Profile', () {}),
                      _buildProfileTile(
                        Icons.verified_user_outlined,
                        isAr ? 'توثيق الحساب' : 'Verify Account',
                        () => context.push('/verification'),
                        trailing: profile.isVerified ? const Icon(Icons.check_circle, color: AppColors.success, size: 20) : null,
                      ),
                      _buildProfileTile(Icons.account_balance_wallet_outlined, isAr ? 'المحفظة الإلكترونية' : 'E-Wallet', () => context.push('/wallet')),
                      _buildSectionTitle(isAr ? 'الإعدادات' : 'Settings'),
                      _buildProfileTile(Icons.language, isAr ? 'اللغة (العربية)' : 'Language', () => AppSettings.toggleLanguage()),
                      _buildProfileTile(Icons.dark_mode_outlined, isAr ? 'المظهر (ليلي/نهاري)' : 'Theme', () => AppSettings.toggleTheme()),
                      _buildProfileTile(Icons.fingerprint, isAr ? 'تفعيل البصمة' : 'Biometrics', () async { await BiometricHelper.authenticate(); }),
                      _buildSectionTitle(isAr ? 'الدعم' : 'Support'),
                      _buildProfileTile(Icons.info_outline, isAr ? 'الشروط والأحكام' : 'Terms', () {}),
                      const SizedBox(height: 30),
                      TextButton.icon(
                        onPressed: () {
                          // Dispatches LogoutRequested → AuthRepositoryImpl calls
                          // POST /auth/logout (blocklists JWT) then clears secure storage.
                          context.read<AuthBloc>().add(LogoutRequested());
                        },
                        icon: const Icon(Icons.logout, color: AppColors.error),
                        label: Text(isAr ? 'تسجيل الخروج' : 'Logout', style: const TextStyle(color: AppColors.error, fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ),
                );
              }
              return const SizedBox.shrink();
            },
          ),
        );
      }
    ),
    );
  }

  Widget _socialIcon(IconData icon, Color color, String url, BuildContext context) {
    return InkWell(onTap: () => LauncherHelper.openUrl(context, url), child: Container(padding: const EdgeInsets.all(12), decoration: BoxDecoration(color: color.withOpacity(0.1), shape: BoxShape.circle, border: Border.all(color: color.withOpacity(0.2))), child: FaIcon(icon, color: color, size: 24)));
  }

  Widget _buildSectionTitle(String title) { return Padding(padding: const EdgeInsets.fromLTRB(20, 20, 20, 10), child: Align(alignment: Alignment.centerRight, child: Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.textGrey)))); }

  Widget _buildProfileTile(IconData icon, String title, VoidCallback onTap, {Widget? trailing}) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: Card(
        elevation: 0,
        margin: EdgeInsets.zero,
        child: ListTile(
          leading: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: AppColors.primaryBlue.withOpacity(0.05), shape: BoxShape.circle),
            child: Icon(icon, color: AppColors.primaryBlue, size: 20),
          ),
          title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
          trailing: trailing ?? const Icon(Icons.arrow_forward_ios, size: 14, color: AppColors.textGrey),
          onTap: onTap,
        ),
      ),
    );
  }
}
