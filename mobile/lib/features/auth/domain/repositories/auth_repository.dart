import '../data/models/user_model.dart';

abstract class AuthRepository {
  /// [mfaCode] is the 6-digit TOTP code — only required when the account has MFA enabled.
  Future<UserModel> login(String email, String password, {String? mfaCode});
  Future<UserModel> signUp({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
  });
  Future<void> logout();

  /// Sends a password-reset email via POST /auth/forgot-password.
  Future<void> forgotPassword(String email);

  /// Resets the password using the token from the email link.
  Future<void> resetPassword({required String token, required String newPassword});
}
