import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/book_provider.dart';
import 'national_library_page.dart';

/// 외부 도서 통합검색 - 카카오 / 네이버 / 국립중앙도서관 (boot1 수업의 MultiBookSearchService 를 앱 화면으로)
/// 일반 회원: 검색 + 원문 보기 / 관리자: 가격 확인 후 쇼핑몰에 바로 등록
class ExternalSearchPage extends ConsumerStatefulWidget {
  const ExternalSearchPage({super.key});

  @override
  ConsumerState<ExternalSearchPage> createState() => _ExternalSearchPageState();
}

class _ExternalSearchPageState extends ConsumerState<ExternalSearchPage> {
  static const Map<String, String> _sources = {'kakao': '카카오', 'naver': '네이버', 'nl': '국립중앙도서관'};
  final TextEditingController _keyword = TextEditingController();
  String _source = 'kakao';
  bool _loading = false;
  String? _error;
  List<Map<String, dynamic>> _items = [];

  @override
  void dispose() {
    _keyword.dispose();
    super.dispose();
  }

  Future<void> _search() async {
    final String k = _keyword.text.trim();
    if (k.isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final List<Map<String, dynamic>> items = await BookApi.searchExternal(_source, k);
      setState(() => _items = items);
    } catch (e) {
      setState(() => _error = errorMessage(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _import(Map<String, dynamic> book) async {
    final TextEditingController price = TextEditingController(text: book['price']?.toString() ?? '');
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('쇼핑몰에 등록'),
        content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(book['title']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          TextField(controller: price, keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: '판매가(원)', border: OutlineInputBorder())),
          const SizedBox(height: 6),
          const Text('재고는 등록 후 도서 상세 > 관리자 메뉴 > 재고 수량 변경에서 입력해 주세요.', style: TextStyle(fontSize: 12, color: Colors.grey)),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('등록')),
        ],
      ),
    );
    final int? p = int.tryParse(price.text.trim());
    price.dispose();
    if (ok != true || !mounted) return;
    try {
      await BookApi.importExternal({...book, 'price': p});
      ref.invalidate(bookListProvider);
      setState(() => book['alreadyInStore'] = true);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('쇼핑몰에 등록했습니다.')));
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isAdmin = ref.watch(authProvider).isAdmin;
    return Scaffold(
      appBar: AppBar(
        title: const Text('외부 도서 검색'),
        actions: [
          TextButton.icon(
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NationalLibraryPage())),
            icon: const Icon(Icons.account_tree_outlined, size: 18),
            label: const Text('KDC 분류'),
          ),
        ],
      ),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 0),
          child: SegmentedButton<String>(
            segments: [for (final e in _sources.entries) ButtonSegment(value: e.key, label: Text(e.value))],
            selected: {_source},
            onSelectionChanged: (s) {
              setState(() => _source = s.first);
              if (_keyword.text.trim().isNotEmpty) _search();
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(12),
          child: TextField(
            controller: _keyword,
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => _search(),
            decoration: InputDecoration(
              hintText: '제목·저자로 검색',
              border: const OutlineInputBorder(),
              isDense: true,
              suffixIcon: IconButton(icon: const Icon(Icons.search), onPressed: _search),
            ),
          ),
        ),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : _error != null
                  ? EmptyView(message: _error!, onRetry: _search)
                  : _items.isEmpty
                      ? const EmptyView(message: '검색어를 입력해 주세요.\n(API 키가 없는 출처는 결과가 비어 있을 수 있어요)', icon: Icons.travel_explore)
                      : ListView.separated(
                          itemCount: _items.length,
                          separatorBuilder: (_, _) => const Divider(height: 1),
                          itemBuilder: (context, i) {
                            final Map<String, dynamic> b = _items[i];
                            final bool inStore = b['alreadyInStore'] == true;
                            return ListTile(
                              leading: ClipRRect(borderRadius: BorderRadius.circular(4), child: NetImage(imageUrl(b['thumbnail']), width: 46, height: 64)),
                              title: Text(b['title']?.toString() ?? '', maxLines: 2, overflow: TextOverflow.ellipsis),
                              subtitle: Text(
                                '${b['authors'] ?? '저자 미상'} · ${b['publisher'] ?? ''}\n${b['publishDate'] ?? ''}${b['price'] != null ? ' · ${won(b['price'])}' : ''}',
                                maxLines: 2,
                              ),
                              isThreeLine: true,
                              trailing: inStore
                                  ? const Chip(label: Text('판매중'))
                                  : isAdmin
                                      ? IconButton(icon: const Icon(Icons.add_business), tooltip: '쇼핑몰 등록', onPressed: () => _import(b))
                                      : null,
                              onTap: (b['link']?.toString() ?? '').startsWith('http')
                                  ? () => launchUrl(Uri.parse(b['link'].toString()), mode: LaunchMode.externalApplication)
                                  : null,
                            );
                          },
                        ),
        ),
      ]),
    );
  }
}
