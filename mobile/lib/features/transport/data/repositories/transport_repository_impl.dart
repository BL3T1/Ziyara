import 'dart:async';
import 'dart:convert';
import 'package:stomp_dart_client/stomp_dart_client.dart';
import '../../../../core/api/api_client.dart';
import '../../../../core/services/token_storage_service.dart';
import '../../../../core/di/injection_container.dart';
import '../../domain/repositories/transport_repository.dart';
import '../models/transport_model.dart';

class TransportRepositoryImpl implements TransportRepository {
  final ApiClient apiClient;

  TransportRepositoryImpl({required this.apiClient});

  @override
  Future<List<TransportModel>> getTransportTypes() async {
    final response = await apiClient.get('/taxi/vehicle-types');
    final data = response.data['data'];
    if (data is List) {
      return data.map((e) => TransportModel.fromJson(e as Map<String, dynamic>)).toList();
    }
    // Fallback: return sensible defaults if endpoint doesn't exist yet
    return [
      const TransportModel(id: 'STANDARD', type: 'اقتصادية', basePrice: 25000, etaMinutes: 5),
      const TransportModel(id: 'VIP', type: 'VIP', basePrice: 50000, etaMinutes: 3),
      const TransportModel(id: 'VAN', type: 'Van', basePrice: 75000, etaMinutes: 8),
    ];
  }

  @override
  Future<String> bookTransport(String from, String to, String type) async {
    final response = await apiClient.post('/taxi/bookings', data: {
      'pickupLocation': from,
      'destinationLocation': to,
      'vehicleType': type,
    });
    return response.data['data']['id']?.toString() ?? '';
  }

  @override
  Stream<DriverModel> trackDriver(String taxiBookingId) {
    final controller = StreamController<DriverModel>.broadcast();

    // Resolve the base WebSocket URL from the API base URL
    final baseUrl = ApiClient.effectiveBaseUrl
        .replaceFirst('https://', 'wss://')
        .replaceFirst('http://', 'ws://')
        .replaceAll('/api/v1', '');

    StompClient? client;
    client = StompClient(
      config: StompConfig(
        url: '$baseUrl/ws/websocket',
        onConnect: (frame) {
          client!.subscribe(
            destination: '/topic/tracking/$taxiBookingId',
            callback: (frame) {
              if (frame.body != null && !controller.isClosed) {
                try {
                  final json = jsonDecode(frame.body!) as Map<String, dynamic>;
                  controller.add(DriverModel.fromJson(json));
                } catch (_) {
                  // ignore malformed frames
                }
              }
            },
          );
        },
        beforeConnect: () async {
          final token = await sl<TokenStorageService>().getAccessToken();
          if (token != null) {
            final headers = client!.config.stompConnectHeaders;
            if (headers != null) {
              headers['Authorization'] = 'Bearer $token';
            }
          }
        },
        onDisconnect: (_) {
          if (!controller.isClosed) controller.close();
        },
        onStompError: (frame) {
          if (!controller.isClosed) {
            controller.addError(Exception('STOMP error: ${frame.body}'));
          }
        },
        onWebSocketError: (error) {
          if (!controller.isClosed) controller.addError(error);
        },
        reconnectDelay: const Duration(seconds: 5),
      ),
    );

    client.activate();

    // Clean up STOMP when the stream has no more listeners
    controller.onCancel = () {
      client?.deactivate();
    };

    return controller.stream;
  }
}
