import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';

/// 내 주문내역 (GET /api/orders?page=1&size=50 → PageResponseDto.content)
final ordersProvider = FutureProvider.autoDispose<List<Map<String, dynamic>>>((ref) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/orders', queryParameters: {'page': 1, 'size': 50});
  final dynamic data = res.data;
  final List<dynamic> list = data is Map ? (data['content'] as List? ?? []) : (data as List);
  return list.map((e) => Map<String, dynamic>.from(e as Map)).toList();
});

class OrderActions {
  /// 결제 전 주문은 삭제, 결제완료/취소 주문은 내 목록에서만 숨김 (서버 정책)
  static Future<void> delete(int orderId) => DioClient.instance.delete('/api/orders/$orderId');
}
