import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../address/postcode_page.dart';
import '../../auth/data/auth_provider.dart';
import '../data/cart_provider.dart';

/// 주문서 - 받는 분 / 휴대폰(결제완료 문자 수신) / 우편번호 검색 주소 → 주문 생성 → 카카오페이 결제창
class CheckoutPage extends ConsumerStatefulWidget {
  final List<Map<String, dynamic>> lines; // {title, cover, price, quantity}
  final List<int>? cartItemIds;
  final int? bookId;
  final int? quantity;

  CheckoutPage.cart({super.key, required List<Map<String, dynamic>> items})
      : lines = items
            .map((i) => {'title': i['bookTitle'], 'cover': i['bookCover'], 'price': i['price'], 'quantity': i['quantity']})
            .toList(),
        cartItemIds = items.map((i) => asInt(i['id'])).toList(),
        bookId = null,
        quantity = null;

  CheckoutPage.direct({super.key, required Map<String, dynamic> book, required int this.quantity})
      : lines = [
          {'title': book['title'], 'cover': book['bookCover'], 'price': book['price'], 'quantity': quantity}
        ],
        cartItemIds = null,
        bookId = asInt(book['id']);

  @override
  ConsumerState<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends ConsumerState<CheckoutPage> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _detail = TextEditingController();
  String _zipcode = '';
  String _address = '';
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _name.text = ref.read(authProvider).nickname;
  }

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _detail.dispose();
    super.dispose();
  }

  int get _total => widget.lines.fold(0, (sum, l) => sum + asInt(l['price']) * asInt(l['quantity']));

  void _toast(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _searchAddress() async {
    final Map<String, String>? r =
        await Navigator.push<Map<String, String>>(context, MaterialPageRoute(builder: (_) => const PostcodePage()));
    if (r == null) return;
    setState(() {
      _zipcode = r['zonecode'] ?? '';
      _address = '${r['address'] ?? ''} ${r['extra'] ?? ''}'.trim();
    });
  }

  Future<void> _pay() async {
    final String phone = _phone.text.replaceAll(RegExp(r'[^0-9]'), '');
    if (_name.text.trim().isEmpty) return _toast('받는 분 이름을 입력해 주세요.');
    if (!RegExp(r'^01[016789]\d{7,8}$').hasMatch(phone)) return _toast('휴대폰 번호를 확인해 주세요.');
    if (_zipcode.isEmpty) return _toast('우편번호 검색으로 주소를 선택해 주세요.');
    if (_detail.text.trim().isEmpty) return _toast('상세주소를 입력해 주세요.');

    setState(() => _busy = true);
    try {
      final int orderId = await OrderApi.createOrder(
        cartItemIds: widget.cartItemIds,
        bookId: widget.bookId,
        quantity: widget.quantity,
        receiverName: _name.text.trim(),
        receiverPhone: phone,
        zipcode: _zipcode,
        address: _address,
        addressDetail: _detail.text.trim(),
      );
      ref.read(cartProvider.notifier).fetch(); // 주문된 장바구니 항목은 서버에서 비워짐
      final String url = await OrderApi.kakaoPayReady(orderId);
      final bool opened = await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
      if (!mounted) return;
      if (!opened) {
        _toast('결제창을 열 수 없습니다. 주문내역에서 다시 결제해 주세요.');
      } else {
        _toast('결제를 마치면 "앱으로 돌아가기"를 눌러 주세요.');
      }
      Navigator.popUntil(context, (r) => r.isFirst);
    } catch (e) {
      _toast(errorMessage(e, fallback: '주문을 만들지 못했습니다.'));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('주문서')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        const Text('주문 상품', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        for (final l in widget.lines)
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(l['title']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
            subtitle: Text('${won(l['price'])} × ${l['quantity']}'),
            trailing: Text(won(asInt(l['price']) * asInt(l['quantity']))),
          ),
        const Divider(height: 28),
        const Text('배송 정보', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        const SizedBox(height: 10),
        TextField(controller: _name, maxLength: 50, decoration: const InputDecoration(labelText: '받는 분', border: OutlineInputBorder(), counterText: '')),
        const SizedBox(height: 10),
        TextField(
          controller: _phone,
          keyboardType: TextInputType.phone,
          decoration: const InputDecoration(labelText: '휴대폰 번호 (결제완료 문자 수신)', hintText: '010-1234-5678', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 10),
        Row(children: [
          Expanded(
            child: InputDecorator(
              decoration: const InputDecoration(labelText: '우편번호', border: OutlineInputBorder()),
              child: Text(_zipcode.isEmpty ? '-' : _zipcode),
            ),
          ),
          const SizedBox(width: 8),
          FilledButton.tonal(onPressed: _searchAddress, child: const Text('우편번호 검색')),
        ]),
        if (_address.isNotEmpty) Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: Text(_address)),
        TextField(controller: _detail, decoration: const InputDecoration(labelText: '상세주소 (동·호수)', border: OutlineInputBorder())),
        const SizedBox(height: 24),
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          const Text('총 결제금액', style: TextStyle(fontSize: 16)),
          Text(won(_total), style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.blue.shade700)),
        ]),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: ElevatedButton(
            onPressed: _busy ? null : _pay,
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFEE500),
              foregroundColor: Colors.black87,
              padding: const EdgeInsets.symmetric(vertical: 15),
            ),
            child: _busy
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('카카오페이로 결제하기', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          ),
        ),
      ),
    );
  }
}
