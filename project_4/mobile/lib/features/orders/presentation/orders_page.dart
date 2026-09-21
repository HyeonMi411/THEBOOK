import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/order_provider.dart';

class OrdersPage extends ConsumerStatefulWidget {
  const OrdersPage({super.key});

  @override
  ConsumerState<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends ConsumerState<OrdersPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(orderProvider.notifier).fetchMyOrders());
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'PAID':
        return Colors.blue;
      case 'CANCELLED':
      case 'FAILED':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    final orderState = ref.watch(orderProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('주문내역')),
      body: orderState.loading && orderState.orders.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : orderState.orders.isEmpty
              ? const Center(child: Text('주문내역이 없습니다.'))
              : RefreshIndicator(
                  onRefresh: () => ref.read(orderProvider.notifier).fetchMyOrders(),
                  child: ListView.builder(
                    itemCount: orderState.orders.length,
                    itemBuilder: (context, index) {
                      final order = orderState.orders[index];
                      final items = (order['items'] as List<dynamic>?) ?? [];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        child: Padding(
                          padding: const EdgeInsets.all(12.0),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text('주문 #${order['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
                                  Text(
                                    order['order_status'] ?? '',
                                    style: TextStyle(color: _statusColor(order['order_status'] ?? ''), fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              ...items.map((item) => Text('- ${item['bookTitle']} x ${item['quantity']}')),
                              const SizedBox(height: 6),
                              Text('결제금액: ${order['total_amount']}원', style: const TextStyle(color: Colors.blue)),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
    );
  }
}
