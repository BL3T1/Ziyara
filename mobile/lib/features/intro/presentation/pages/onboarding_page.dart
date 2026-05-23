import 'package:flutter/material.dart';
import 'package:introduction_screen/introduction_screen.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../../core/theme/app_colors.dart';

class OnboardingPage extends StatelessWidget {
  const OnboardingPage({super.key});

  Future<void> _onIntroEnd(BuildContext context) async {
    // حفظ أن المستخدم قد رأى الشرح ولن يظهر له مرة أخرى
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isFirstTime', false);
    
    if (context.mounted) {
      context.go('/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    const bodyStyle = TextStyle(fontSize: 18.0, color: AppColors.textGrey);
    const pageDecoration = PageDecoration(
      titleTextStyle: TextStyle(fontSize: 28.0, fontWeight: FontWeight.bold, color: AppColors.primaryBlue),
      bodyTextStyle: bodyStyle,
      bodyPadding: EdgeInsets.fromLTRB(16.0, 0.0, 16.0, 16.0),
      pageColor: Colors.white,
      imagePadding: EdgeInsets.zero,
      imageFlex: 2, // الصورة تأخذ مساحة أكبر
    );

    return IntroductionScreen(
      globalBackgroundColor: Colors.white,
      allowImplicitScrolling: true,
      autoScrollDuration: 5000,
      
      pages: [
        PageViewModel(
          title: "اكتشف سوريا بجمالها",
          body: "استمتع بزيارة أجمل المعالم السياحية والأثرية في سوريا مع خدمات حجز متكاملة.",
          image: _buildImage(Icons.landscape_rounded),
          decoration: pageDecoration,
        ),
        PageViewModel(
          title: "حجوزات سهلة وآمنة",
          body: "احجز الفنادق، المطاعم، والسيارات بضغطة زر مع خيارات دفع متعددة وآمنة.",
          image: _buildImage(Icons.touch_app_rounded),
          decoration: pageDecoration,
        ),
        PageViewModel(
          title: "رفيق سفرك الدائم",
          body: "خدمة عملاء على مدار الساعة، وتتبع مباشر لرحلاتك لضمان راحتك.",
          image: _buildImage(Icons.support_agent_rounded),
          decoration: pageDecoration,
        ),
      ],
      onDone: () => _onIntroEnd(context),
      onSkip: () => _onIntroEnd(context), // زر تخطي
      showSkipButton: true,
      skipOrBackFlex: 0,
      nextFlex: 0,
      showBackButton: false,
      rtl: true, // دعم العربية
      // تخصيص الأزرار
      skip: const Text('تخطي', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.textGrey)),
      next: const Icon(Icons.arrow_forward, color: AppColors.primaryDark),
      done: const Text('ابدأ الآن', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.primaryDark)),
      curve: Curves.fastLinearToSlowEaseIn,
      controlsMargin: const EdgeInsets.all(16),
      controlsPadding: const EdgeInsets.fromLTRB(8.0, 4.0, 8.0, 4.0),
      dotsDecorator: const DotsDecorator(
        size: Size(10.0, 10.0),
        color: Color(0xFFBDBDBD),
        activeSize: Size(22.0, 10.0),
        activeShape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(25.0)),
        ),
        activeColor: AppColors.gold, // لون النقاط
      ),
    );
  }

  Widget _buildImage(IconData icon) {
    return Container(
      padding: const EdgeInsets.all(40),
      decoration: BoxDecoration(
        color: AppColors.background,
        shape: BoxShape.circle,
      ),
      child: Icon(icon, size: 100, color: AppColors.primaryBlue),
    );
  }
}
