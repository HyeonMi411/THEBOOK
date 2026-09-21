import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../shared/components/app_layout.dart';
import '../../../core/network/api_client.dart';
import '../data/book_provider.dart';
import 'book_detail_page.dart';

class BookListPage extends ConsumerStatefulWidget {
  const BookListPage({super.key});

  @override
  ConsumerState<BookListPage> createState() => _BookListPageState();
}

class _BookListPageState extends ConsumerState<BookListPage> {
  String _resolveImageUrl(String? url) {
    if (url == null || url.isEmpty) return '';
    if (url.startsWith('http')) return url;
    final base = ApiClient.getBaseUrl();
    return url.startsWith('/') ? '$base$url' : '$base/$url';
  }

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(bookProvider.notifier).fetchBooks());
  }

  @override
  Widget build(BuildContext context) {
    final bookState = ref.watch(bookProvider);

    return AppLayout(
      child: bookState.loading && bookState.books.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : bookState.books.isEmpty
              ? const Center(child: Text('등록된 도서가 없습니다.'))
              : RefreshIndicator(
                  onRefresh: () => ref.read(bookProvider.notifier).fetchBooks(),
                  child: GridView.builder(
                    padding: const EdgeInsets.all(12),
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      childAspectRatio: 0.62,
                      crossAxisSpacing: 10,
                      mainAxisSpacing: 10,
                    ),
                    itemCount: bookState.books.length,
                    itemBuilder: (context, index) {
                      final book = bookState.books[index];
                      return InkWell(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(builder: (_) => BookDetailPage(bookId: book['id'])),
                          );
                        },
                        child: Card(
                          clipBehavior: Clip.antiAlias,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Image.network(
                                  _resolveImageUrl(book['cover']),
                                  fit: BoxFit.cover,
                                  width: double.infinity,
                                  errorBuilder: (_, __, ___) => Container(
                                    color: Colors.grey[200],
                                    alignment: Alignment.center,
                                    child: const Icon(Icons.menu_book, color: Colors.grey),
                                  ),
                                ),
                              ),
                              Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(book['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                                    const SizedBox(height: 2),
                                    Text('${book['price'] ?? 0}원',
                                        style: const TextStyle(color: Colors.blue, fontSize: 13)),
                                  ],
                                ),
                              ),
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
