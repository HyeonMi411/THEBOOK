import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/cart_provider.dart';
import 'checkout_page.dart';

/// 장바구니 - 3차처럼 체크박스로 주문할 도서를 고름 (처음엔 주문 가능한 도서 전체 선택)
class CartPage extends ConsumerStatefulWidget {
  const CartPage({super.key});

  @override
  ConsumerState<CartPage> createState() => _CartPageState();
}

class _CartPageState extends ConsumerState<CartPage> {
  final Set<int> _selected = {};
  final Set<int> _seen = {}; // 한 번이라도 목록에 나온 항목 (새로 담긴 항목만 자동 선택)

  static bool _buyable(Map<String, dynamic> i) =>
      i['bookDeleted'] != true && asInt(i['stockQuantity']) >= asInt(i['quantity']);

  void _sync(List<Map<String, dynamic>> items) {
    final Set<int> ids = items.map((i) => asInt(i['id'])).toSet();
    _selected.removeWhere((id) => !ids.contains(id));
    for (final Map<String, dynamic> i in items) {
      final int id = asInt(i['id']);
      if (!_buyable(i)) {
        _selected.remove(id);
      } else if (_seen.add(id)) {
        _selected.add(id);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool loggedIn = ref.watch(authProvider.select((a) => a.isLoggedIn));
    final CartState cart = ref.watch(cartProvider);
    _sync(cart.items);
    return AppLayout(title: '장바구니', child: _body(context, ref, loggedIn, cart));
  }

  Widget _body(BuildContext context, WidgetRef ref, bool loggedIn, CartState cart) {
    if (!loggedIn) return const EmptyView(message: '로그인하면 장바구니를 이용할 수 있어요.', icon: Icons.lock_outline);
    if (cart.loading && cart.items.isEmpty) return const Center(child: CircularProgressIndicator());
    if (cart.error != null && cart.items.isEmpty) {
      return EmptyView(message: cart.error!, onRetry: () => ref.read(cartProvider.notifier).fetch());
    }
    if (cart.items.isEmpty) return const EmptyView(message: '장바구니가 비어 있습니다.', icon: Icons.shopping_cart_outlined);

    final List<Map<String, dynamic>> buyable = cart.items.where(_buyable).toList();
    final List<Map<String, dynamic>> chosen = buyable.where((i) => _selected.contains(asInt(i['id']))).toList();
    final int chosenTotal = chosen.fold(0, (sum, i) => sum + asInt(i['subtotal']));
    final int chosenCount = chosen.fold(0, (sum, i) => sum + asInt(i['quantity']));
    final bool allChecked = buyable.isNotEmpty && chosen.length == buyable.length;
    return Column(children: [
      CheckboxListTile(
        controlAffinity: ListTileControlAffinity.leading,
        value: allChecked,
        onChanged: buyable.isEmpty
            ? null
            : (v) => setState(() {
                  if (v == true) {
                    _selected.addAll(buyable.map((i) => asInt(i['id'])));
                  } else {
                    _selected.clear();
                  }
                }),
        title: Text('전체 선택 (${chosen.length}/${buyable.length})'),
        dense: true,
      ),
      const Divider(height: 1),
      Expanded(
        child: RefreshIndicator(
          onRefresh: () => ref.read(cartProvider.notifier).fetch(),
          child: ListView.builder(
            itemCount: cart.items.length,
            itemBuilder: (context, i) {
              final Map<String, dynamic> item = cart.items[i];
              final int id = asInt(item['id']);
              return _CartItemTile(
                item: item,
                selected: _selected.contains(id),
                onSelected: _buyable(item) ? (v) => setState(() => v ? _selected.add(id) : _selected.remove(id)) : null,
              );
            },
          ),
        ),
      ),
      SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(children: [
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Text('선택한 도서 ($chosenCount권)', style: const TextStyle(fontSize: 16)),
              Text(won(chosenTotal), style: TextStyle(fontSize: 19, fontWeight: FontWeight.bold, color: Colors.blue.shade700)),
            ]),
            if (buyable.length < cart.items.length)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text('판매중단·재고부족 상품은 주문에서 제외됩니다.', style: TextStyle(fontSize: 12, color: Colors.red)),
              ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: chosen.isEmpty
                    ? null
                    : () => Navigator.push(context, MaterialPageRoute(builder: (_) => CheckoutPage.cart(items: chosen))),
                style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                child: Text(chosen.isEmpty ? '주문할 도서를 선택해 주세요' : '선택한 도서 주문하기'),
              ),
            ),
          ]),
        ),
      ),
    ]);
  }
}

class _CartItemTile extends ConsumerWidget {
  final Map<String, dynamic> item;
  final bool selected;
  final void Function(bool)? onSelected; // null 이면 선택 불가 (판매중단·재고부족)
  const _CartItemTile({required this.item, required this.selected, this.onSelected});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final int id = asInt(item['id']);
    final int qty = asInt(item['quantity']);
    final int stock = asInt(item['stockQuantity']);
    final bool deleted = item['bookDeleted'] == true;
    final CartNotifier cart = ref.read(cartProvider.notifier);

    Future<void> change(int q) async {
      final String? err = await cart.updateQuantity(id, q);
      if (err != null && context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Row(children: [
          Checkbox(value: selected, onChanged: onSelected == null ? null : (v) => onSelected!(v ?? false)),
          ClipRRect(borderRadius: BorderRadius.circular(4), child: NetImage(imageUrl(item['bookCover']), width: 56, height: 78)),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(item['bookTitle']?.toString() ?? '', maxLines: 2, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w600)),
              Text(won(item['price']), style: const TextStyle(color: Colors.grey)),
              if (deleted) const Text('판매 중단된 도서', style: TextStyle(color: Colors.red, fontSize: 12))
              else if (stock < qty) Text('재고 부족 (남은 재고 $stock권)', style: const TextStyle(color: Colors.red, fontSize: 12)),
              Row(children: [
                IconButton(visualDensity: VisualDensity.compact, onPressed: qty > 1 ? () => change(qty - 1) : null,
                    icon: const Icon(Icons.remove_circle_outline)),
                Text('$qty'),
                IconButton(visualDensity: VisualDensity.compact, onPressed: !deleted && qty < stock ? () => change(qty + 1) : null,
                    icon: const Icon(Icons.add_circle_outline)),
                const Spacer(),
                Text(won(item['subtotal']), style: const TextStyle(fontWeight: FontWeight.bold)),
              ]),
            ]),
          ),
          IconButton(icon: const Icon(Icons.close), onPressed: () => cart.remove(id)),
        ]),
      ),
    );
  }
}
