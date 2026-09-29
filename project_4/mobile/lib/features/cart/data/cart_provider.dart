import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../auth/data/auth_provider.dart';

class CartState {
  final List<Map<String, dynamic>> items;
  final int totalAmount;
  final bool loading;
  final String? error;

  const CartState({this.items = const [], this.totalAmount = 0, this.loading = false, this.error});
}

/// 장바구니 (GET/POST/PATCH/DELETE /api/cart)
class CartNotifier extends Notifier<CartState> {
  Dio get _dio => DioClient.instance;

  @override
  CartState build() {
    // 로그인/로그아웃이 바뀌면 장바구니를 다시 불러오거나 비운다
    final bool loggedIn = ref.watch(authProvider.select((a) => a.isLoggedIn));
    if (loggedIn) {
      Future.microtask(fetch);
      return const CartState(loading: true);
    }
    return const CartState();
  }

  Future<void> fetch() async {
    try {
      final Response<dynamic> res = await _dio.get('/api/cart');
      final Map<dynamic, dynamic> data = res.data as Map;
      state = CartState(
        items: (data['items'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList(),
        totalAmount: (data['totalAmount'] as num?)?.toInt() ?? 0,
      );
    } catch (e) {
      state = CartState(items: state.items, totalAmount: state.totalAmount, error: errorMessage(e));
    }
  }

  Future<String?> add(int bookId, int quantity) async {
    try {
      await _dio.post('/api/cart', data: {'bookId': bookId, 'quantity': quantity});
      await fetch();
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '장바구니에 담지 못했습니다.');
    }
  }

  Future<String?> updateQuantity(int itemId, int quantity) async {
    try {
      await _dio.patch('/api/cart/$itemId', data: {'quantity': quantity});
      await fetch();
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '수량을 바꾸지 못했습니다.');
    }
  }

  Future<void> remove(int itemId) async {
    try {
      await _dio.delete('/api/cart/$itemId');
    } catch (_) {}
    await fetch();
  }
}

final cartProvider = NotifierProvider<CartNotifier, CartState>(CartNotifier.new);

/// 주문 + 결제 API
class OrderApi {
  static Dio get _dio => DioClient.instance;

  /// cartItemIds 또는 (bookId + quantity) 중 하나 + 배송정보 → 주문 id
  static Future<int> createOrder({
    List<int>? cartItemIds,
    int? bookId,
    int? quantity,
    required String receiverName,
    required String receiverPhone,
    required String zipcode,
    required String address,
    required String addressDetail,
  }) async {
    final Response<dynamic> res = await _dio.post('/api/orders', data: {
      'cartItemIds': ?cartItemIds,
      'bookId': ?bookId,
      'quantity': ?quantity,
      'receiverName': receiverName,
      'receiverPhone': receiverPhone,
      'zipcode': zipcode,
      'address': address,
      'addressDetail': addressDetail,
    });
    return ((res.data as Map)['id'] as num).toInt();
  }

  /// 카카오페이 결제 준비 → 결제창 URL (모바일 URL 우선)
  static Future<String> kakaoPayReady(int orderId) async {
    final Response<dynamic> res = await _dio.post('/api/payments/kakao/ready', data: {'orderId': orderId});
    final Map<dynamic, dynamic> data = res.data as Map;
    final String? mobile = data['redirectMobileUrl'] as String?;
    return (mobile != null && mobile.isNotEmpty) ? mobile : data['redirectUrl'] as String;
  }
}
