import 'package:equatable/equatable.dart';
import 'dart:io';
import '../data/models/profile_model.dart';

abstract class ProfileEvent extends Equatable {
  const ProfileEvent();

  @override
  List<Object?> get props => [];
}

class FetchProfile extends ProfileEvent {}

class UpdateProfileDetails extends ProfileEvent {
  final String name;
  final String phone;
  const UpdateProfileDetails({required this.name, required this.phone});

  @override
  List<Object?> get props => [name, phone];
}

class SubmitAccountVerification extends ProfileEvent {
  final File idFront;
  final File idBack;
  const SubmitAccountVerification({required this.idFront, required this.idBack});

  @override
  List<Object?> get props => [idFront, idBack];
}

abstract class ProfileState extends Equatable {
  const ProfileState();

  @override
  List<Object?> get props => [];
}

class ProfileInitial extends ProfileState {}

class ProfileLoading extends ProfileState {}

class ProfileLoaded extends ProfileState {
  final ProfileModel profile;
  const ProfileLoaded(this.profile);

  @override
  List<Object?> get props => [profile];
}

class ProfileUpdateSuccess extends ProfileState {
  final ProfileModel profile;
  const ProfileUpdateSuccess(this.profile);

  @override
  List<Object?> get props => [profile];
}

class VerificationSubmitSuccess extends ProfileState {}

class ProfileError extends ProfileState {
  final String message;
  const ProfileError(this.message);

  @override
  List<Object?> get props => [message];
}
