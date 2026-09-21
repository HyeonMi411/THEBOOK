import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../shared/components/app_layout.dart';
import '../data/cart_provider.dart';

class CartPage extends ConsumerStatefulWidget {
  const CartPage({super.key});

  @override
  ConsumerState<CartPage> createState() => _CartPageState();
}

class _CartPageState extends ConsumerState<CartPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(cartProvider.notifier).fetchCart());
  }

  // 장바구니 전체를 주문 생성 → 카카오페이 결제 준비 → 결제창(외부 브라우저) 오픈까지 진행
  Future<void> _checkout(List<dynamic> items) async {
    final itemIds = items.map<int>((i) => i['id'] as int).toList();
    final notifier = ref.read(cartProvider.notifier);

    final orderId = await notifier.createOrder(itemIds);
    if (orderId == null) {
      _showError('주문 생성에 실패했습니다.');
      return;
    }
    final redirectUrl = await notifier.kakaoPayReady(orderId);
    if (redirectUrl == null) {
      _showError('카카오페이 결제 준비에 실패했습니다.');
      return;
    }
    final uri = Uri.parse(redirectUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      _showError('결제창을 열 수 없습니다.');
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final cartState = ref.watch(cartProvider);
    final cart = cartState.cart;
    final items = (cart?['items'] as List<dynamic>?) ?? [];
    final total = cart?['totalAmount'] ?? 0;

    return AppLayout(
      child: cartState.loading && cart == null
          ? const Center(child: CircularProgressIndicator())
          : items.isEmpty
              ? const Center(child: Text('장바구니가 비어있습니다.'))
              : Column(
                  children: [
                    Expanded(
                      child: ListView.builder(
                        itemCount: items.length,
                        itemBuilder: (context, index) {
                          final item = items[index];
                          return Card(
                            margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            child: ListTile(
                              title: Text(item['bookTitle'] ?? ''),
                              subtitle: Text('${item['price']}원 x ${item['quantity']}'),
                              trailing: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text('${item['subtotal']}원', style: const TextStyle(fontWeight: FontWeight.bold)),
                                  IconButton(
                                    icon: const Icon(Icons.delete_outline, color: Colors.red),
                                    onPressed: () => ref.read(cartProvider.notifier).removeItem(item['id']),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                    SafeArea(
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                const Text('총 결제금액', style: TextStyle(fontSize: 16)),
                                Text('$total원', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.blue)),
                              ],
                            ),
                            const SizedBox(height: 12),
                            SizedBox(
                              width: double.infinity,
                              child: ElevatedButton(
                                style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFFEE500)),
                                onPressed: () => _checkout(items),
                                child: const Text('카카오페이로 결제하기', style: TextStyle(color: Colors.black87)),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
    );
  }
}
