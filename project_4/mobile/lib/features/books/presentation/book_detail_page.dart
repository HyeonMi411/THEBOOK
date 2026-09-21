import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../data/book_provider.dart';
import '../../cart/data/cart_provider.dart';
import '../../auth/data/auth_provider.dart';

class BookDetailPage extends ConsumerStatefulWidget {
  final int bookId;
  const BookDetailPage({super.key, required this.bookId});

  @override
  ConsumerState<BookDetailPage> createState() => _BookDetailPageState();
}

class _BookDetailPageState extends ConsumerState<BookDetailPage> {
  Map<String, dynamic>? _book;
  bool _loading = true;

  String _resolveImageUrl(String? url) {
    if (url == null || url.isEmpty) return '';
    if (url.startsWith('http')) return url;
    final base = ApiClient.getBaseUrl();
    return url.startsWith('/') ? '$base$url' : '$base/$url';
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final data = await ref.read(bookProvider.notifier).fetchBookDetail(widget.bookId);
    setState(() {
      _book = data;
      _loading = false;
    });
  }

  Future<void> _addToCart() async {
    final authState = ref.read(authProvider);
    if (authState.accessToken == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('로그인이 필요한 서비스입니다.')));
      Navigator.pushNamed(context, '/login');
      return;
    }
    final ok = await ref.read(cartProvider.notifier).addToCart(widget.bookId);
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok ? '장바구니에 담았습니다.' : '장바구니 담기에 실패했습니다.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_book == null) {
      return const Scaffold(body: Center(child: Text('도서 정보를 불러올 수 없습니다.')));
    }
    final book = _book!;

    return Scaffold(
      appBar: AppBar(title: Text(book['title'] ?? '도서 상세')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.network(
                  _resolveImageUrl(book['cover']),
                  height: 260,
                  errorBuilder: (_, __, ___) => Container(
                    height: 260,
                    width: 180,
                    color: Colors.grey[200],
                    alignment: Alignment.center,
                    child: const Icon(Icons.menu_book, size: 48, color: Colors.grey),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(book['title'] ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text('${book['author'] ?? ''} · ${book['publisher'] ?? ''}', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 8),
            Text('${book['price'] ?? 0}원', style: const TextStyle(fontSize: 18, color: Colors.blue, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text('재고: ${book['stock_quantity'] ?? 0}권'),
            const Divider(height: 32),
            Text(book['description'] ?? '설명이 없습니다.'),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: ElevatedButton(
            onPressed: _addToCart,
            child: const Text('장바구니 담기'),
          ),
        ),
      ),
    );
  }
}
