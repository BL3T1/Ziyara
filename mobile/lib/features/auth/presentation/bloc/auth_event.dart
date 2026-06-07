import 'package:equatable/equatable.dart';

abstract class AuthEvent extends Equatable {
  const AuthEvent();

  @override
  List<Object?> get props => [];
}

class LoginRequested extends AuthEvent {
  final String email;
  final String password;

  const LoginRequested({required this.email, required this.password});

  @override
  List<Object?> get props => [email, password];
}

/// Submitted when the user enters their 6-digit TOTP code on the MFA challenge screen.
class SubmitMfaCode extends AuthEvent {
  final String email;
  final String password;
  final String mfaCode;

  const SubmitMfaCode({
    required this.email,
    required this.password,
    required this.mfaCode,
  });

  @override
  List<Object?> get props => [email, password, mfaCode];
}

class LogoutRequested extends AuthEvent {}

class SignUpRequested extends AuthEvent {
  final String email;
  final String password;
  final String firstName;
  final String lastName;
  final String? phone;
  final String? dateOfBirth;

  const SignUpRequested({
    required this.email,
    required this.password,
    required this.firstName,
    required this.lastName,
    this.phone,
    this.dateOfBirth,
  });

  @override
  List<Object?> get props => [email, password, firstName, lastName, phone, dateOfBirth];
}
