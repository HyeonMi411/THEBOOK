import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/book_provider.dart';

/// 국립중앙도서관 도서 검색 - 3차 /books/national-library 와 같은 구성
/// 키워드 검색 + KDC(한국십진분류법) 분류 아코디언(하위 분류를 누르면 그 분류명으로 검색) + 관리자 "BookStore에 저장"
class NationalLibraryPage extends ConsumerStatefulWidget {
  const NationalLibraryPage({super.key});

  @override
  ConsumerState<NationalLibraryPage> createState() => _NationalLibraryPageState();
}

class _NationalLibraryPageState extends ConsumerState<NationalLibraryPage> {
  static const List<(String, List<String>)> kdcGroups = [
    ('000 총류', ['001 지식, 학문 일반', '003 사전', '004 컴퓨터과학', '005 프로그래밍, 소프트웨어', '006 특허, 표준', '007 정보학', '008 총서', '009 기타 총류']),
    ('100 철학', ['110 형이상학', '120 인식론', '130 논리학', '140 윤리학', '150 심리학', '160 미학', '170 동양철학', '180 서양철학', '190 기타 철학']),
    ('200 종교', ['210 비교종교', '220 불교', '230 기독교', '240 천주교', '250 도교', '260 이슬람교', '270 힌두교', '280 기타 종교']),
    ('300 사회과학', ['310 통계학', '320 경제학', '330 경영학', '340 법학', '350 행정학', '360 사회학', '370 교육학', '380 풍속, 민속학', '390 정치학']),
    ('400 자연과학', ['410 수학', '420 물리학', '430 화학', '440 천문학', '450 지학', '460 생물학', '470 식물학', '480 동물학', '490 기타 자연과학']),
    ('500 기술과학', ['510 의학', '520 공학일반', '530 건축공학', '540 기계공학', '550 전기전자공학', '560 화학공학', '570 제조업', '580 생활과학', '590 농업, 축산업']),
    ('600 예술', ['610 건축예술', '620 조각', '630 회화', '640 사진', '650 음악', '660 연극', '670 영화', '680 오락, 스포츠']),
    ('700 언어', ['710 한국어', '720 중국어', '730 일본어', '740 영어', '750 독일어', '760 프랑스어', '770 스페인어', '780 기타 언어']),
    ('800 문학', ['810 한국문학', '820 중국문학', '830 일본문학', '840 영어문학', '850 독일문학', '860 프랑스문학', '870 스페인문학', '880 기타 문학']),
    ('900 역사', ['910 한국사', '920 아시아사', '930 유럽사', '940 아메리카사', '950 아프리카사', '960 오세아니아사', '970 고고학', '980 전기', '990 기타 역사']),
  ];

  final TextEditingController _keyword = TextEditingController();
  List<Map<String, dynamic>> _results = [];
  String? _label;     // 결과 제목에 표시할 검색어/분류
  String? _error;
  bool _loading = false;
  int _page = 1;
  bool _showKdc = true;

  @override
  void dispose() {
    _keyword.dispose();
    super.dispose();
  }

  Future<void> _search(String keyword, {String? label, int page = 1}) async {
    final String k = keyword.trim();
    if (k.isEmpty) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _loading = true;
      _error = null;
      _label = label ?? '"$k"';
      _page = page;
      if (page == 1) _results = [];
    });
    try {
      final List<Map<String, dynamic>> list = await BookApi.nlSearch(k, page: page);
      setState(() {
        _results = page == 1 ? list : [..._results, ...list];
        _showKdc = false; // 결과가 오면 분류표는 접어서 결과가 바로 보이게
      });
    } catch (e) {
      setState(() => _error = errorMessage(e, fallback: '국립중앙도서관 검색에 실패했습니다. (NL_API_KEY 설정 확인)'));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _lastQuery = '';

  void _run(String keyword, {String? label}) {
    _lastQuery = keyword;
    _search(keyword, label: label);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('국립중앙도서관 도서 검색')),
      body: ListView(padding: const EdgeInsets.all(12), children: [
        const Text('국립중앙도서관 소장자료를 검색합니다. 관리자는 검색 결과를 BookStore 에 바로 저장할 수 있어요.',
            style: TextStyle(color: Colors.grey, fontSize: 13)),
        const SizedBox(height: 10),
        TextField(
          controller: _keyword,
          textInputAction: TextInputAction.search,
          onSubmitted: (v) => _run(v),
          decoration: InputDecoration(
            hintText: '도서명·저자·키워드',
            border: const OutlineInputBorder(),
            isDense: true,
            suffixIcon: IconButton(icon: const Icon(Icons.search), onPressed: () => _run(_keyword.text)),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          margin: EdgeInsets.zero,
          child: ExpansionTile(
            key: ValueKey('kdc-$_showKdc'),
            initiallyExpanded: _showKdc,
            onExpansionChanged: (v) => _showKdc = v,
            leading: const Icon(Icons.account_tree_outlined),
            title: const Text('KDC 전체 분류 체계', style: TextStyle(fontWeight: FontWeight.bold)),
            children: [
              for (int i = 0; i < kdcGroups.length; i++)
                ExpansionTile(
                  initiallyExpanded: i == 0, // 3차와 같이 "000 총류"만 펼친 상태로 시작
                  title: Text(kdcGroups[i].$1, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                  childrenPadding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
                  children: [
                    Wrap(spacing: 6, runSpacing: 6, children: [
                      for (final String item in kdcGroups[i].$2)
                        ActionChip(
                          label: Text(item, style: const TextStyle(fontSize: 12)),
                          onPressed: () {
                            _keyword.clear();
                            _run(item, label: '분류: $item');
                          },
                        ),
                    ]),
                  ],
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        if (_label != null) Text('검색결과 ($_label)', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        if (_error != null) EmptyView(message: _error!, onRetry: () => _search(_lastQuery, label: _label)),
        if (_error == null && !_loading && _label != null && _results.isEmpty)
          const EmptyView(message: '검색 결과가 없습니다.', icon: Icons.search_off),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: _results.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2, childAspectRatio: 0.62, crossAxisSpacing: 10, mainAxisSpacing: 10),
          itemBuilder: (_, i) => _card(_results[i]),
        ),
        if (_loading) const Padding(padding: EdgeInsets.all(20), child: Center(child: CircularProgressIndicator())),
        if (!_loading && _results.isNotEmpty && _results.length % 10 == 0)
          TextButton(onPressed: () => _search(_lastQuery, label: _label, page: _page + 1), child: const Text('더 보기')),
      ]),
    );
  }

  /// 서버 BookNlDto 는 image_url 원본을 내려주므로 표지가 없으면 아이콘
  String _cover(Map<String, dynamic> b) {
    final String u = b['image_url']?.toString() ?? '';
    return (u.isEmpty || u == 'http://cover.nl.go.kr/') ? '' : u;
  }

  Widget _card(Map<String, dynamic> b) {
    return Card(
      clipBehavior: Clip.antiAlias,
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: () => _detail(b),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(child: NetImage(_cover(b), width: double.infinity, fallbackIcon: Icons.menu_book)),
          Padding(
            padding: const EdgeInsets.all(8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(b['title_info']?.toString() ?? '', maxLines: 2, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
              Text(b['author_info']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: Colors.grey, fontSize: 12)),
              if ((b['kdc_name_1s']?.toString() ?? '').isNotEmpty)
                Text(b['kdc_name_1s'].toString(), style: const TextStyle(color: Color(0xFF2563EB), fontSize: 12, fontWeight: FontWeight.w600)),
            ]),
          ),
        ]),
      ),
    );
  }

  /// 상세 (3차 /books/national-library/[id]) - 관리자는 BookStore 에 저장
  void _detail(Map<String, dynamic> b) {
    final bool isAdmin = ref.read(authProvider).isAdmin;
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.7,
        builder: (_, sc) => ListView(controller: sc, padding: const EdgeInsets.all(20), children: [
          Center(child: ClipRRect(borderRadius: BorderRadius.circular(8), child: NetImage(_cover(b), width: 140, height: 200))),
          const SizedBox(height: 14),
          Text(b['title_info']?.toString() ?? '', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          for (final (String, String) row in [
            ('저자', 'author_info'), ('출판사', 'pub_info'), ('발행년도', 'pub_year_info'), ('ISBN', 'isbn'), ('분류', 'kdc_name_1s'), ('페이지', 'page_info'), ('가격', 'price_info'),
          ])
            if ((b[row.$2]?.toString() ?? '').isNotEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 3),
                child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  SizedBox(width: 70, child: Text(row.$1, style: const TextStyle(color: Colors.grey))),
                  Expanded(child: Text(b[row.$2].toString())),
                ]),
              ),
          if ((b['subject_info']?.toString() ?? '').isNotEmpty)
            Padding(padding: const EdgeInsets.only(top: 10), child: Text(b['subject_info'].toString(), style: const TextStyle(height: 1.6))),
          const SizedBox(height: 18),
          if (isAdmin)
            FilledButton.icon(
              icon: const Icon(Icons.download),
              label: const Text('BookStore에 저장'),
              onPressed: () async {
                try {
                  final Map<String, dynamic> saved = await BookApi.nlSave(b);
                  ref.invalidate(bookListProvider);
                  if (ctx.mounted) Navigator.pop(ctx);
                  if (mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                        content: Text("'${saved['title']}' 을(를) 저장했습니다. 가격·재고는 도서 상세 > 관리자 메뉴에서 입력해 주세요.")));
                  }
                } catch (e) {
                  if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
                }
              },
            ),
        ]),
      ),
    );
  }
}
