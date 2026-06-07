import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../../../core/error/backend_exception.dart';
import 'auth_event.dart';
import 'auth_state.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthRepository repository;

  AuthBloc({required this.repository}) : super(AuthInitial()) {
    on<LoginRequested>(_onLoginRequested);
    on<SubmitMfaCode>(_onSubmitMfaCode);
    on<LogoutRequested>(_onLogoutRequested);
    on<SignUpRequested>(_onSignUpRequested);
  }

  Future<void> _onLoginRequested(
    LoginRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    try {
      final user = await repository.login(event.email, event.password);
      emit(AuthAuthenticated(user));
    } catch (e) {
      if (e is BackendException && e.code == 'MFA_CODE_REQUIRED') {
        // Account has TOTP enabled — navigate to MFA challenge screen
        emit(AuthMfaRequired(email: event.email, password: event.password));
        return;
      }
      final message = e is BackendException
          ? e.userMessage
          : 'حدث خطأ أثناء تسجيل الدخول، يرجى المحاولة مجدداً';
      emit(AuthError(message));
    }
  }

  Future<void> _onSubmitMfaCode(
    SubmitMfaCode event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    try {
      final user = await repository.login(
        event.email,
        event.password,
        mfaCode: event.mfaCode,
      );
      emit(AuthAuthenticated(user));
    } catch (e) {
      final message = e is BackendException
          ? e.userMessage
          : 'رمز التحقق غير صحيح، يرجى المحاولة مجدداً';
      // Return to MFA screen then surface the error
      emit(AuthMfaRequired(email: event.email, password: event.password));
      emit(AuthError(message));
    }
  }

  Future<void> _onLogoutRequested(
    LogoutRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    try {
      await repository.logout();
    } catch (_) {
      // Even if the backend call fails, clear the local session
    }
    emit(AuthUnauthenticated());
  }

  Future<void> _onSignUpRequested(
    SignUpRequested event,
    Emitter<AuthState> emit,
  ) async {
    emit(AuthLoading());
    try {
      await repository.signUp(
        email: event.email,
        password: event.password,
        firstName: event.firstName,
        lastName: event.lastName,
        phone: event.phone,
        dateOfBirth: event.dateOfBirth,
      );
      emit(AuthSignedUp(event.email));
    } catch (e) {
      final message = e is BackendException
          ? e.userMessage
          : 'حدث خطأ أثناء إنشاء الحساب، يرجى المحاولة مجدداً';
      emit(AuthError(message));
    }
  }
}
