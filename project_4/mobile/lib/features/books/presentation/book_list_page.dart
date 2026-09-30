import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/book_provider.dart';
import 'book_form_page.dart';
import 'book_detail_page.dart';
import 'external_search_page.dart';

class BookListPage extends ConsumerStatefulWidget {
  const BookListPage({super.key});

  @override
  ConsumerState<BookListPage> createState() => _BookListPageState();
}

class _BookListPageState extends ConsumerState<BookListPage> {
  final TextEditingController _search = TextEditingController();
  final ScrollController _scroll = ScrollController();
  Timer? _debounce;
  List<Map<String, dynamic>> _suggestions = [];
  int _suggestSeq = 0; // 늦게 도착한 이전 요청 결과가 최신 결과를 덮어쓰지 않도록

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) {
        ref.read(bookListProvider.notifier).loadMore();
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _search.dispose();
    _scroll.dispose();
    super.dispose();
  }

  /// 실시간 검색 (3차 BookSearchBox 와 같은 방식: 입력 멈춘 뒤 300ms → /api/books/search)
  void _onTyping(String value) {
    _debounce?.cancel();
    final String k = value.trim();
    if (k.isEmpty) {
      setState(() => _suggestions = []);
      return;
    }
    _debounce = Timer(const Duration(milliseconds: 300), () async {
      final int seq = ++_suggestSeq;
      try {
        final List<Map<String, dynamic>> list = await BookApi.suggest(k);
        if (mounted && seq == _suggestSeq && _search.text.trim() == k) setState(() => _suggestions = list);
      } catch (_) {
        if (mounted && seq == _suggestSeq) setState(() => _suggestions = []);
      }
    });
  }

  void _doSearch(String keyword) {
    _debounce?.cancel();
    _suggestSeq++;
    setState(() => _suggestions = []);
    _search.text = keyword;
    FocusScope.of(context).unfocus();
    ref.read(bookListProvider.notifier).search(keyword);
  }

  /// 표지 사진으로 검색 - 카메라/앨범 → CLOVA OCR → 추천 검색어 확인 → 제목 검색
  Future<void> _ocrSearch() async {
    if (!requireLogin(context, ref)) return;
    final ImageSource? source = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(leading: const Icon(Icons.photo_camera), title: const Text('표지 촬영'), onTap: () => Navigator.pop(ctx, ImageSource.camera)),
          ListTile(leading: const Icon(Icons.photo_library), title: const Text('앨범에서 선택'), onTap: () => Navigator.pop(ctx, ImageSource.gallery)),
        ]),
      ),
    );
    if (source == null) return;
    final XFile? image = await ImagePicker().pickImage(source: source, maxWidth: 1600, imageQuality: 85);
    if (image == null || !mounted) return;

    showDialog(context: context, barrierDismissible: false, builder: (_) => const Center(child: CircularProgressIndicator()));
    Map<String, dynamic>? result;
    String? error;
    try {
      result = await BookApi.ocr(image);
    } catch (e) {
      error = errorMessage(e, fallback: '사진에서 글자를 읽지 못했습니다.');
    }
    if (!mounted) return;
    Navigator.pop(context); // 로딩 닫기
    if (error != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error)));
      return;
    }
    final List<String> lines = ((result!['lines'] as List?) ?? []).map((e) => e.toString()).toList();
    final String? picked = await showDialog<String>(
      context: context,
      builder: (ctx) => SimpleDialog(
        title: const Text('어떤 글자로 검색할까요?'),
        children: [
          for (final String line in {result!['keyword'].toString(), ...lines}.take(8))
            SimpleDialogOption(onPressed: () => Navigator.pop(ctx, line), child: Text(line)),
        ],
      ),
    );
    if (picked != null && picked.isNotEmpty) _doSearch(picked);
  }

  @override
  Widget build(BuildContext context) {
    final BookListState st = ref.watch(bookListProvider);
    final bool isAdmin = ref.watch(authProvider).isAdmin;
    return AppLayout(
      title: '도서',
      floatingActionButton: isAdmin
          ? FloatingActionButton.extended(
              heroTag: 'fab-book-create',
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const BookFormPage())),
              icon: const Icon(Icons.add),
              label: const Text('도서 등록'),
            )
          : null,
      actions: [
        IconButton(icon: const Icon(Icons.photo_camera_outlined), tooltip: '표지 사진으로 검색', onPressed: _ocrSearch),
        IconButton(
          icon: const Icon(Icons.travel_explore),
          tooltip: '외부 도서 검색',
          onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ExternalSearchPage())),
        ),
      ],
      child: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
          child: TextField(
            controller: _search,
            textInputAction: TextInputAction.search,
            onSubmitted: _doSearch,
            onChanged: _onTyping,
            decoration: InputDecoration(
              hintText: '도서 제목 검색',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: st.keyword.isNotEmpty || _search.text.isNotEmpty
                  ? IconButton(icon: const Icon(Icons.close), onPressed: () => _doSearch(''))
                  : null,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
              isDense: true,
            ),
          ),
        ),
        Expanded(
          child: Stack(children: [
            _body(st),
            if (_suggestions.isNotEmpty) _suggestBox(),
          ]),
        ),
      ]),
    );
  }

  /// 자동완성 드롭다운 - 제목/저자, 누르면 상세로 이동, 맨 아래 "전체 결과 보기"
  Widget _suggestBox() {
    return Positioned(
      left: 12,
      right: 12,
      top: 0,
      child: Material(
        elevation: 6,
        borderRadius: BorderRadius.circular(10),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxHeight: 380),
          child: ListView(shrinkWrap: true, padding: EdgeInsets.zero, children: [
            for (final Map<String, dynamic> b in _suggestions)
              ListTile(
                dense: true,
                leading: ClipRRect(borderRadius: BorderRadius.circular(3), child: NetImage(imageUrl(b['bookCover']), width: 32, height: 44)),
                title: Text(b['title']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
                subtitle: Text('${b['author'] ?? ''} · ${b['price'] == null ? '가격 미정' : won(b['price'])}', maxLines: 1),
                onTap: () {
                  setState(() => _suggestions = []);
                  FocusScope.of(context).unfocus();
                  Navigator.push(context, MaterialPageRoute(builder: (_) => BookDetailPage(bookId: asInt(b['id']))));
                },
              ),
            ListTile(
              dense: true,
              tileColor: const Color(0xFFF8FAFF),
              title: Text("'${_search.text.trim()}' 전체 결과 보기",
                  textAlign: TextAlign.center, style: const TextStyle(color: Color(0xFF2563EB), fontWeight: FontWeight.w600)),
              onTap: () => _doSearch(_search.text),
            ),
          ]),
        ),
      ),
    );
  }

  Widget _body(BookListState st) {
    if (st.books.isEmpty && st.loading) return const Center(child: CircularProgressIndicator());
    if (st.books.isEmpty && st.error != null) {
      return EmptyView(message: st.error!, icon: Icons.wifi_off, onRetry: () => ref.read(bookListProvider.notifier).refresh());
    }
    if (st.books.isEmpty) {
      return EmptyView(
        message: st.keyword.isEmpty ? '등록된 도서가 없습니다.' : "'${st.keyword}' 검색 결과가 없습니다.\n외부 도서 검색(오른쪽 위 🌐)을 이용해 보세요.",
      );
    }
    return RefreshIndicator(
      onRefresh: () => ref.read(bookListProvider.notifier).refresh(),
      child: GridView.builder(
        controller: _scroll,
        padding: const EdgeInsets.all(12),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2, childAspectRatio: 0.6, crossAxisSpacing: 10, mainAxisSpacing: 10),
        itemCount: st.books.length + (st.hasMore ? 1 : 0),
        itemBuilder: (context, i) {
          if (i >= st.books.length) return const Center(child: CircularProgressIndicator());
          return BookCard(book: st.books[i]);
        },
      ),
    );
  }
}

class BookCard extends StatelessWidget {
  final Map<String, dynamic> book;
  final String? badge;
  const BookCard({super.key, required this.book, this.badge});

  @override
  Widget build(BuildContext context) {
    final int stock = asInt(book['stockQuantity']);
    return InkWell(
      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => BookDetailPage(bookId: asInt(book['id'])))),
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            child: Stack(fit: StackFit.expand, children: [
              NetImage(imageUrl(book['bookCover'])),
              if (badge != null)
                Positioned(
                  left: 6, top: 6,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    color: Colors.blue.shade700,
                    child: Text(badge!, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
                  ),
                ),
              if (stock <= 0)
                Container(color: Colors.black38, alignment: Alignment.center,
                    child: const Text('품절', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold))),
            ]),
          ),
          Padding(
            padding: const EdgeInsets.all(8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(book['title']?.toString() ?? '', maxLines: 2, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              const SizedBox(height: 2),
              Text(book['author']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: Colors.grey, fontSize: 12)),
              Text(book['price'] == null ? '가격 미정' : won(book['price']),
                  style: TextStyle(color: Colors.blue.shade700, fontSize: 13, fontWeight: FontWeight.w600)),
            ]),
          ),
        ]),
      ),
    );
  }
}
