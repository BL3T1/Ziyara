import 'package:equatable/equatable.dart';

class ProfileModel extends Equatable {
  final String id;
  final String name;
  final String email;
  final String phone;
  final double walletBalance;
  final bool isVerified;
  final String? profileImageUrl;

  const ProfileModel({
    required this.id,
    required this.name,
    required this.email,
    required this.phone,
    required this.walletBalance,
    required this.isVerified,
    this.profileImageUrl,
  });

  factory ProfileModel.fromJson(Map<String, dynamic> json) {
    return ProfileModel(
      id: json['id'],
      name: json['name'],
      email: json['email'],
      phone: json['phone'],
      walletBalance: (json['wallet_balance'] ?? 0.0).toDouble(),
      isVerified: json['is_verified'] ?? false,
      profileImageUrl: json['profile_image_url'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'email': email,
      'phone': phone,
      'wallet_balance': walletBalance,
      'is_verified': isVerified,
      'profile_image_url': profileImageUrl,
    };
  }

  @override
  List<Object?> get props => [id, name, email, phone, walletBalance, isVerified, profileImageUrl];
}
