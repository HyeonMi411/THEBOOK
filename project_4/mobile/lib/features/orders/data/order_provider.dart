// 전역상태관리: 내 주문내역 (마이페이지)
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/network/api_client.dart';

class OrderState {
  final List<dynamic> orders;
  final bool loading;
  final String? error;

  const OrderState({this.orders = const [], this.loading = false, this.error});
}

class OrderNotifier extends Notifier<OrderState> {
  @override
  OrderState build() {
    _dio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
    _dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) async {
      final token = await _storage.read(key: 'accessToken');
      if (token != null) options.headers['Authorization'] = 'Bearer $token';
      return handler.next(options);
    }));
    return const OrderState();
  }

  late final Dio _dio;
  final _storage = const FlutterSecureStorage();

  // 내 주문내역 조회 (GET /api/orders, 12개씩 페이징)
  Future<void> fetchMyOrders() async {
    state = OrderState(orders: state.orders, loading: true, error: null);
    try {
      final response = await _dio.get('/api/orders');
      final data = response.data;
      final list = (data is Map && data.containsKey('results')) ? data['results'] : data;
      state = OrderState(orders: list ?? [], loading: false, error: null);
    } catch (err) {
      state = OrderState(orders: state.orders, loading: false, error: '주문내역 조회 실패: ${err.toString()}');
    }
  }
}

final orderProvider = NotifierProvider<OrderNotifier, OrderState>(() {
  return OrderNotifier();
});
