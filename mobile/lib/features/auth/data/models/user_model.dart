import 'package:equatable/equatable.dart';

class UserModel extends Equatable {
  final String id;
  final String email;
  final String role;
  final String? phone;
  final String? firstName;
  final String? lastName;

  const UserModel({
    required this.id,
    required this.email,
    required this.role,
    this.phone,
    this.firstName,
    this.lastName,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: (json['id'] ?? json['userId']) as String,
      email: json['email'] as String,
      role: json['role'] as String,
      phone: json['phone'] as String?,
      firstName: json['firstName'] as String?,
      lastName: json['lastName'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'role': role,
      'phone': phone,
      'firstName': firstName,
      'lastName': lastName,
    };
  }

  @override
  List<Object?> get props => [id, email, role, phone, firstName, lastName];
}
