import 'package:equatable/equatable.dart';
import '../../data/models/user_model.dart';

abstract class AuthState extends Equatable {
  const AuthState();

  @override
  List<Object?> get props => [];
}

class AuthInitial extends AuthState {}

class AuthLoading extends AuthState {}

class AuthAuthenticated extends AuthState {
  final UserModel user;

  const AuthAuthenticated(this.user);

  @override
  List<Object?> get props => [user];
}

class AuthUnauthenticated extends AuthState {}

class AuthError extends AuthState {
  final String message;

  const AuthError(this.message);

  @override
  List<Object?> get props => [message];
}

class AuthSignedUp extends AuthState {
  final String email;
  const AuthSignedUp(this.email);
  @override
  List<Object?> get props => [email];
}

/// Emitted when the backend requires a TOTP code (account has MFA enabled).
/// The login page navigates to [MfaChallengeScreen] on this state.
/// Stores credentials so they can be re-submitted with the code.
class AuthMfaRequired extends AuthState {
  final String email;
  final String password;

  const AuthMfaRequired({required this.email, required this.password});

  @override
  List<Object?> get props => [email, password];
}
