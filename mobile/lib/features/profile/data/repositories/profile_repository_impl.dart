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
    final response = await apiClient.get('/users/me');
    final data = response.data['data'] as Map<String, dynamic>? ?? response.data as Map<String, dynamic>;
    return ProfileModel.fromJson(data);
  }

  @override
  Future<ProfileModel> updateProfile(Map<String, dynamic> data) async {
    final response = await apiClient.put('/users/me', data: data);
    final responseData = response.data['data'] as Map<String, dynamic>? ?? response.data as Map<String, dynamic>;
    return ProfileModel.fromJson(responseData);
  }

  @override
  Future<void> submitVerification(File idFront, File idBack) async {
    final formData = FormData.fromMap({
      'id_front': await MultipartFile.fromFile(idFront.path),
      'id_back': await MultipartFile.fromFile(idBack.path),
    });
    await apiClient.post('/users/me/verification', data: formData);
  }
}
