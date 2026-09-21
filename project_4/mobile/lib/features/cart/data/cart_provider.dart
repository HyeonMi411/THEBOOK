// 전역상태관리: 장바구니 + 주문/카카오페이 결제
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/network/api_client.dart';

class CartState {
  final Map<String, dynamic>? cart;
  final bool loading;
  final String? error;

  const CartState({this.cart, this.loading = false, this.error});
}

class CartNotifier extends Notifier<CartState> {
  @override
  CartState build() {
    _initDio();
    return const CartState();
  }

  late final Dio _dio;
  final _storage = const FlutterSecureStorage();

  void _initDio() {
    _dio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await _storage.read(key: 'accessToken');
        if (token != null) options.headers['Authorization'] = 'Bearer $token';
        return handler.next(options);
      },
      onError: (DioException e, handler) async {
        if (e.response?.statusCode == 401) {
          final refreshed = await _refreshAccessToken();
          if (refreshed) {
            final newToken = await _storage.read(key: 'accessToken');
            e.requestOptions.headers['Authorization'] = 'Bearer $newToken';
            try {
              final cloned = await _dio.fetch(e.requestOptions);
              return handler.resolve(cloned);
            } catch (_) {}
          }
        }
        return handler.next(e);
      },
    ));
  }

  Future<bool> _refreshAccessToken() async {
    try {
      final refreshDio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
      final response = await refreshDio.post('/auth/refresh');
      final newAccessToken = response.data['accessToken'];
      if (newAccessToken != null) {
        await _storage.write(key: 'accessToken', value: newAccessToken);
        return true;
      }
      return false;
    } catch (_) {
      return false;
    }
  }

  // 1. 장바구니 조회 (GET /api/cart)
  Future<void> fetchCart() async {
    state = CartState(cart: state.cart, loading: true, error: null);
    try {
      final response = await _dio.get('/api/cart');
      state = CartState(cart: response.data, loading: false, error: null);
    } catch (err) {
      state = CartState(cart: state.cart, loading: false, error: '장바구니 조회 실패: ${err.toString()}');
    }
  }

  // 2. 장바구니 담기 (POST /api/cart)
  Future<bool> addToCart(int bookId, {int quantity = 1}) async {
    try {
      await _dio.post('/api/cart', data: {'bookId': bookId, 'quantity': quantity});
      await fetchCart();
      return true;
    } catch (err) {
      state = CartState(cart: state.cart, loading: false, error: '담기 실패: ${err.toString()}');
      return false;
    }
  }

  // 3. 장바구니 항목 수량 수정 (PATCH /api/cart/{itemId})
  Future<bool> updateQuantity(int itemId, int quantity) async {
    try {
      await _dio.patch('/api/cart/$itemId', data: {'quantity': quantity});
      await fetchCart();
      return true;
    } catch (err) {
      state = CartState(cart: state.cart, loading: false, error: '수량 변경 실패: ${err.toString()}');
      return false;
    }
  }

  // 4. 장바구니 항목 삭제 (DELETE /api/cart/{itemId})
  Future<bool> removeItem(int itemId) async {
    try {
      await _dio.delete('/api/cart/$itemId');
      await fetchCart();
      return true;
    } catch (_) {
      return false;
    }
  }

  // 5. 주문 생성 (POST /api/orders, cartItemIds 기반)
  Future<int?> createOrder(List<int> cartItemIds) async {
    try {
      final response = await _dio.post('/api/orders', data: {'cartItemIds': cartItemIds});
      return response.data['id'] as int?;
    } catch (err) {
      state = CartState(cart: state.cart, loading: false, error: '주문 생성 실패: ${err.toString()}');
      return null;
    }
  }

  // 6. 카카오페이 결제 준비 (POST /api/orders/payments/kakao/ready)
  // 반환된 redirectUrl을 외부 브라우저(url_launcher)로 열면 결제창이 뜸
  Future<String?> kakaoPayReady(int orderId) async {
    try {
      final response = await _dio.post('/api/orders/payments/kakao/ready', data: {'orderId': orderId});
      return response.data['redirectUrl'] as String?;
    } catch (err) {
      state = CartState(cart: state.cart, loading: false, error: '결제 준비 실패: ${err.toString()}');
      return null;
    }
  }
}

final cartProvider = NotifierProvider<CartNotifier, CartState>(() {
  return CartNotifier();
});
