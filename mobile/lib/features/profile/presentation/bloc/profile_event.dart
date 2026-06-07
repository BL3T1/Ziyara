import 'package:equatable/equatable.dart';
import 'dart:io';

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
