import 'package:flutter/material.dart';

class ZiyaraLogoPainter extends CustomPainter {
  final Color color;
  ZiyaraLogoPainter({this.color = Colors.white});

  @override
  void paint(Canvas canvas, Size size) {
    final Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.width * 0.03
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    // رسم جسم الحقيبة
    final rect = RRect.fromRectAndRadius(
      Rect.fromCenter(center: Offset(size.width / 2, size.height * 0.6), width: size.width * 0.9, height: size.height * 0.55),
      Radius.circular(size.width * 0.1),
    );
    canvas.drawRRect(rect, paint);

    // رسم المقبض
    final handlePath = Path();
    handlePath.moveTo(size.width * 0.35, size.height * 0.32);
    handlePath.quadraticBezierTo(size.width * 0.35, size.height * 0.15, size.width * 0.5, size.height * 0.15);
    handlePath.quadraticBezierTo(size.width * 0.65, size.height * 0.15, size.width * 0.65, size.height * 0.32);
    canvas.drawPath(handlePath, paint);

    // رسم الزخرفة
    final wavePath = Path();
    wavePath.moveTo(size.width * 0.2, size.height * 0.75);
    wavePath.quadraticBezierTo(size.width * 0.4, size.height * 0.65, size.width * 0.6, size.height * 0.75);
    wavePath.quadraticBezierTo(size.width * 0.8, size.height * 0.85, size.width * 0.8, size.height * 0.75);
    
    final wavePaint = Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = size.width * 0.015..strokeCap = StrokeCap.round;
    canvas.drawPath(wavePath, wavePaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
