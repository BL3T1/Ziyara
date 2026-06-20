/// Represents a structured error response from the Ziyara backend.
///
/// Backend error shape:
/// ```json
/// { "success": false, "code": "INVALID_CREDENTIALS", "message": "..." }
/// ```
class BackendException implements Exception {
  final String code;
  final String rawMessage;

  const BackendException({required this.code, required this.rawMessage});

  /// Human-readable Arabic message shown to the user.
  String get userMessage => switch (code) {
        'INVALID_CREDENTIALS' => 'البريد الإلكتروني أو كلمة المرور غير صحيحة',
        'MFA_ENROLLMENT_REQUIRED' =>
          'يجب تفعيل المصادقة الثنائية (TOTP) لهذا الحساب',
        'MFA_CODE_REQUIRED' => 'مطلوب رمز التحقق الثنائي',
        'MFA_CODE_INVALID'   => 'رمز المصادقة الثنائية غير صحيح أو منتهي',
        'AUTH_FAILED'        => 'البريد الإلكتروني أو كلمة المرور غير صحيحة',
        'BOOKING_CONFLICT' => 'هذا التاريخ محجوز مسبقاً، يرجى اختيار تاريخ آخر',
        'DISCOUNT_CODE_INVALID' => 'رمز الخصم غير صالح أو منتهي الصلاحية',
        'DISCOUNT_CODE_EXPIRED' => 'رمز الخصم منتهي الصلاحية',
        'RESOURCE_NOT_FOUND' => 'العنصر المطلوب غير موجود',
        'UNAUTHORIZED' => 'غير مصرح لك بالقيام بهذا الإجراء',
        'TOKEN_EXPIRED' => 'انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً',
        'NO_INTERNET' => 'لا يوجد اتصال بالإنترنت، تحقق من الشبكة',
        'PASSWORD_TOO_WEAK' => 'كلمة المرور ضعيفة جداً، يرجى استخدام كلمة مرور أقوى',
        'EMAIL_ALREADY_EXISTS' => 'البريد الإلكتروني مستخدم مسبقاً',
        'BOOKING_NOT_CANCELLABLE' => 'لا يمكن إلغاء هذا الحجز في الوقت الحالي',
        'SERVICE_UNAVAILABLE' => 'الخدمة غير متاحة حالياً، يرجى المحاولة لاحقاً',
        _ => rawMessage.isNotEmpty ? rawMessage : 'حدث خطأ غير متوقع',
      };

  @override
  String toString() => 'BackendException($code): $rawMessage';
}
