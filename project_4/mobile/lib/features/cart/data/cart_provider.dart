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

  /// 결제창 방식 - 실행할 때 --dart-define=PAY_MODE=pc 를 주면 QR 결제창(PC 용 URL)을 연다.
  ///  - 기본(mobile): 카카오톡 앱으로 결제 → 실제 휴대폰용
  ///  - pc          : 브라우저에 QR 코드 → 다른 휴대폰의 카카오톡으로 찍어서 결제
  ///                  (카카오톡이 없는 에뮬레이터에서 테스트할 때. 모바일 URL 은 Play 스토어로 이동해 버림)
  static const bool _qrPayment = String.fromEnvironment('PAY_MODE') == 'pc';

  /// 카카오페이 결제 준비 → 결제창 URL
  static Future<String> kakaoPayReady(int orderId) async {
    final Response<dynamic> res = await _dio.post('/api/payments/kakao/ready', data: {'orderId': orderId});
    final Map<dynamic, dynamic> data = res.data as Map;
    final String? mobile = data['redirectMobileUrl'] as String?;
    final String? pc = data['redirectUrl'] as String?;
    if (_qrPayment && pc != null && pc.isNotEmpty) return pc;
    return (mobile != null && mobile.isNotEmpty) ? mobile : pc!;
  }
}
