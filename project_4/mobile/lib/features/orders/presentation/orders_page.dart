import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../cart/data/cart_provider.dart';
import '../data/order_provider.dart';

class OrdersPage extends ConsumerWidget {
  const OrdersPage({super.key});

  static const Map<String, (String, Color)> _status = {
    'PENDING': ('결제 대기', Colors.orange),
    'PAID': ('결제 완료', Colors.green),
    'CANCELLED': ('취소', Colors.grey),
    'FAILED': ('결제 실패', Colors.red),
  };

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> orders = ref.watch(ordersProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('주문내역')),
      body: orders.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e), onRetry: () => ref.invalidate(ordersProvider)),
        data: (list) => list.isEmpty
            ? const EmptyView(message: '주문내역이 없습니다.', icon: Icons.receipt_long)
            : RefreshIndicator(
                onRefresh: () async => ref.invalidate(ordersProvider),
                child: ListView.builder(
                  itemCount: list.length,
                  itemBuilder: (context, i) => _orderCard(context, ref, list[i]),
                ),
              ),
      ),
    );
  }

  Widget _orderCard(BuildContext context, WidgetRef ref, Map<String, dynamic> o) {
    final String status = o['orderStatus']?.toString() ?? '';
    final (String, Color) s = _status[status] ?? (status, Colors.grey);
    final List<dynamic> items = o['items'] as List? ?? [];
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Text('주문번호 ${o['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(width: 8),
            Chip(label: Text(s.$1, style: TextStyle(color: s.$2)), visualDensity: VisualDensity.compact),
            const Spacer(),
            Text(shortDateTime(o['approvedAt'] ?? o['createdAt']), style: const TextStyle(color: Colors.grey, fontSize: 12)),
          ]),
          for (final dynamic it in items)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 2),
              child: Text('· ${it['bookTitle']}  ${won(it['price'])} × ${it['quantity']}'),
            ),
          if (o['address'] != null)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text('배송지 (${o['zipcode'] ?? ''}) ${o['address']} ${o['addressDetail'] ?? ''}\n받는 분 ${o['receiverName'] ?? '-'}',
                  style: const TextStyle(fontSize: 12, color: Colors.black54)),
            ),
          const Divider(),
          Row(children: [
            Text('합계 ${won(o['totalAmount'])}', style: const TextStyle(fontWeight: FontWeight.bold)),
            const Spacer(),
            if (status == 'PENDING')
              TextButton(
                onPressed: () async {
                  try {
                    final String url = await OrderApi.kakaoPayReady(asInt(o['id']));
                    await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
                  } catch (e) {
                    if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
                  }
                },
                child: const Text('결제하기'),
              ),
            TextButton(
              onPressed: () async {
                final bool? ok = await showDialog<bool>(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    content: Text(status == 'PENDING' ? '결제 전 주문을 삭제할까요?' : '내 주문내역에서 숨길까요? (결제 기록은 보관됩니다)'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
                      FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('확인')),
                    ],
                  ),
                );
                if (ok != true) return;
                try {
                  await OrderActions.delete(asInt(o['id']));
                } catch (e) {
                  if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
                }
                ref.invalidate(ordersProvider);
              },
              child: Text(status == 'PENDING' ? '삭제' : '숨기기', style: const TextStyle(color: Colors.grey)),
            ),
          ]),
        ]),
      ),
    );
  }
}
