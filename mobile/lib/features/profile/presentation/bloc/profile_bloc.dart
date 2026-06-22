import 'package:flutter_bloc/flutter_bloc.dart';
import '../../domain/repositories/profile_repository.dart';
import 'profile_event.dart';
import 'profile_state.dart';

class ProfileBloc extends Bloc<ProfileEvent, ProfileState> {
  final ProfileRepository repository;

  ProfileBloc({required this.repository}) : super(ProfileInitial()) {
    on<FetchProfile>(_onFetchProfile);
    on<UpdateProfileDetails>(_onUpdateProfileDetails);
    on<SubmitAccountVerification>(_onSubmitAccountVerification);
  }

  Future<void> _onFetchProfile(FetchProfile event, Emitter<ProfileState> emit) async {
    emit(ProfileLoading());
    try {
      final profile = await repository.getProfile();
      emit(ProfileLoaded(profile));
    } catch (e) {
      emit(ProfileError(e.toString()));
    }
  }

  Future<void> _onUpdateProfileDetails(UpdateProfileDetails event, Emitter<ProfileState> emit) async {
    emit(ProfileLoading());
    try {
      final profile = await repository.updateProfile({
        'name': event.name,
        'phone': event.phone,
      });
      emit(ProfileUpdateSuccess(profile));
      emit(ProfileLoaded(profile));
    } catch (e) {
      emit(ProfileError(e.toString()));
    }
  }

  Future<void> _onSubmitAccountVerification(SubmitAccountVerification event, Emitter<ProfileState> emit) async {
    emit(ProfileLoading());
    try {
      await repository.submitVerification(event.idFront, event.idBack);
      emit(VerificationSubmitSuccess());
      // Re-fetch profile to show pending verification status if needed
      final profile = await repository.getProfile();
      emit(ProfileLoaded(profile));
    } catch (e) {
      emit(ProfileError(e.toString()));
    }
  }
}
