import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../../cart/data/cart_provider.dart';
import '../../cart/presentation/checkout_page.dart';
import '../data/book_provider.dart';
import 'book_form_page.dart';
import '../../../shared/text_input_dialog.dart';

class BookDetailPage extends ConsumerStatefulWidget {
  final int bookId;
  const BookDetailPage({super.key, required this.bookId});

  @override
  ConsumerState<BookDetailPage> createState() => _BookDetailPageState();
}

class _BookDetailPageState extends ConsumerState<BookDetailPage> {
  int _qty = 1;

  Future<void> _addToCart() async {
    if (!requireLogin(context, ref)) return;
    final String? err = await ref.read(cartProvider.notifier).add(widget.bookId, _qty);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(err ?? '장바구니에 담았습니다.'),
      action: err == null ? SnackBarAction(label: '보기', onPressed: () => Navigator.popUntil(context, (r) => r.isFirst)) : null,
    ));
  }

  void _buyNow(Map<String, dynamic> book) {
    if (!requireLogin(context, ref)) return;
    Navigator.push(context, MaterialPageRoute(
      builder: (_) => CheckoutPage.direct(book: book, quantity: _qty),
    ));
  }

  // ── 관리자: 수정 / 재고 변경 / 삭제 (3차 도서 상세의 관리자 버튼) ──
  Future<void> _admin(String action, Map<String, dynamic> book) async {
    if (action == 'edit') {
      await Navigator.push(context, MaterialPageRoute(builder: (_) => BookFormPage(editing: book)));
      return;
    }
    if (action == 'stock') {
      final String? input = await showTextInputDialog(
        context,
        title: '재고 수량 변경',
        initialValue: '${book['stockQuantity'] ?? 0}',
        keyboardType: TextInputType.number,
        suffix: '권',
        confirmText: '저장',
      );
      if (input == null || !mounted) return;
      final int? qty = int.tryParse(input);
      if (qty == null || qty < 0) return _toast('재고는 0 이상의 숫자로 입력해 주세요.');
      try {
        await BookApi.updateStock(widget.bookId, qty);
        ref.invalidate(bookDetailProvider(widget.bookId));
        ref.invalidate(bookListProvider);
        _toast('재고를 $qty권으로 바꿨습니다.');
      } catch (e) {
        _toast(errorMessage(e));
      }
      return;
    }
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('도서 삭제'),
        content: Text("'${book['title']}' 을(를) 삭제할까요?\n이미 결제된 주문내역은 그대로 남습니다."),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
          FilledButton(style: FilledButton.styleFrom(backgroundColor: Colors.red), onPressed: () => Navigator.pop(ctx, true), child: const Text('삭제')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await BookApi.deleteBook(widget.bookId);
      ref.invalidate(bookListProvider);
      if (!mounted) return;
      _toast('도서를 삭제했습니다.');
      Navigator.pop(context);
    } catch (e) {
      _toast(errorMessage(e));
    }
  }

  void _toast(String m) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<Map<String, dynamic>> detail = ref.watch(bookDetailProvider(widget.bookId));
    final bool isAdmin = ref.watch(authProvider).isAdmin;
    return Scaffold(
      appBar: AppBar(
        title: const Text('도서 상세'),
        actions: [
          if (isAdmin)
            detail.maybeWhen(
              data: (book) => PopupMenuButton<String>(
                tooltip: '관리자 메뉴',
                icon: const Icon(Icons.admin_panel_settings_outlined),
                onSelected: (v) => _admin(v, book),
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'edit', child: Text('도서 수정')),
                  PopupMenuItem(value: 'stock', child: Text('재고 수량 변경')),
                  PopupMenuItem(value: 'delete', child: Text('도서 삭제', style: TextStyle(color: Colors.red))),
                ],
              ),
              orElse: () => const SizedBox.shrink(),
            ),
        ],
      ),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: '도서 정보를 불러올 수 없습니다.', onRetry: () => ref.invalidate(bookDetailProvider(widget.bookId))),
        data: (book) {
          final int stock = asInt(book['stockQuantity']);
          return ListView(padding: const EdgeInsets.all(16), children: [
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: NetImage(imageUrl(book['bookCover']), height: 260, width: 180),
              ),
            ),
            const SizedBox(height: 16),
            Text(book['title']?.toString() ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text('${book['author'] ?? ''} · ${book['publisher'] ?? ''}', style: const TextStyle(color: Colors.grey)),
            if (book['publishDate'] != null) Text('출간일 ${book['publishDate']}', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 10),
            Row(children: [
              Text(book['price'] == null ? '가격 미정' : won(book['price']),
                  style: TextStyle(fontSize: 20, color: Colors.blue.shade700, fontWeight: FontWeight.bold)),
              const Spacer(),
              Chip(label: Text(book['category']?.toString() ?? '기타')),
            ]),
            Text(stock > 0 ? '재고 $stock권' : '품절', style: TextStyle(color: stock > 0 ? Colors.black87 : Colors.red)),
            const Divider(height: 32),
            Text(
              (book['description']?.toString().trim().isNotEmpty ?? false) ? book['description'].toString() : '소개글이 없습니다.',
              style: const TextStyle(height: 1.6),
            ),
          ]);
        },
      ),
      bottomNavigationBar: detail.maybeWhen(
        data: (book) {
          final int stock = asInt(book['stockQuantity']);
          final bool canBuy = stock > 0 && book['price'] != null;
          return SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(children: [
                IconButton(onPressed: _qty > 1 ? () => setState(() => _qty--) : null, icon: const Icon(Icons.remove_circle_outline)),
                Text('$_qty', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                IconButton(onPressed: _qty < stock ? () => setState(() => _qty++) : null, icon: const Icon(Icons.add_circle_outline)),
                const SizedBox(width: 8),
                Expanded(child: OutlinedButton(onPressed: canBuy ? _addToCart : null, child: const Text('장바구니'))),
                const SizedBox(width: 8),
                Expanded(child: FilledButton(onPressed: canBuy ? () => _buyNow(book) : null, child: const Text('바로구매'))),
              ]),
            ),
          );
        },
        orElse: () => null,
      ),
    );
  }
}
