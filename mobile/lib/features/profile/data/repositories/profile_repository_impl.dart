import 'dart:io';
import 'package:dio/dio.dart';
import '../../../../core/api/api_client.dart';
import '../models/profile_model.dart';
import '../../domain/repositories/profile_repository.dart';

class ProfileRepositoryImpl implements ProfileRepository {
  final ApiClient apiClient;

  ProfileRepositoryImpl({required this.apiClient});

  @override
  Future<ProfileModel> getProfile() async {
    final response = await apiClient.get('/profile');
    return ProfileModel.fromJson(response.data);
  }

  @override
  Future<ProfileModel> updateProfile(Map<String, dynamic> data) async {
    final response = await apiClient.put('/profile', data: data);
    return ProfileModel.fromJson(response.data);
  }

  @override
  Future<void> submitVerification(File idFront, File idBack) async {
    final formData = FormData.fromMap({
      'id_front': await MultipartFile.fromFile(idFront.path),
      'id_back': await MultipartFile.fromFile(idBack.path),
    });
    await apiClient.post('/profile/verify', data: formData);
  }
}
