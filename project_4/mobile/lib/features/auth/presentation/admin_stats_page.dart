import 'package:flutter/material.dart';

import '../../../core/network/dio_client.dart';

/// 관리자 운영 통계 - Django(pandas) 가 Oracle 을 집계한 결과를 Spring Boot(/api/admin/stats) 를 거쳐 받아 그린다.
/// 웹 대시보드와 같은 집계 함수를 쓰므로 숫자가 항상 같다. 그래프는 패키지 없이 기본 위젯으로 그림.
class AdminStatsPage extends StatefulWidget {
  const AdminStatsPage({super.key});

  @override
  State<AdminStatsPage> createState() => _AdminStatsPageState();
}

class _AdminStatsPageState extends State<AdminStatsPage> {
  Map<String, dynamic>? _data;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await DioClient.instance.get('/api/admin/stats');
      final dynamic d = res.data;
      if (d is Map) {
        _data = Map<String, dynamic>.from(d);
        if (_data!['db_error'] is String) _error = _data!['db_error'] as String;
      } else {
        _error = '통계 형식이 올바르지 않습니다.';
      }
    } catch (e) {
      _error = errorMessage(e, fallback: '통계를 불러오지 못했습니다.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('운영 통계'),
        actions: [IconButton(icon: const Icon(Icons.refresh), tooltip: '새로고침', onPressed: _loading ? null : _load)],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
                children: [
                  if (_error != null)
                    Card(
                      color: Colors.red.shade50,
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Text(_error!, style: TextStyle(color: Colors.red.shade700)),
                      ),
                    ),
                  if (_data != null) ..._sections(context, _data!),
                  const SizedBox(height: 12),
                  const Text('Django(pandas)가 Oracle 데이터를 집계한 결과입니다. 아래로 당기면 새로고침됩니다.',
                      style: TextStyle(fontSize: 12, color: Colors.grey)),
                ],
              ),
            ),
    );
  }

  List<Widget> _sections(BuildContext context, Map<String, dynamic> d) {
    final ColorScheme cs = Theme.of(context).colorScheme;
    final Map sales = _m(d['sales']);
    final Map summary = _m(sales['stats_summary']);
    final Map community = _m(d['community']);
    final Map cSummary = _m(community['community_summary']);
    final Map chatbot = _m(d['chatbot']);
    final Map ops = _m(d['ops']);

    return [
      // ① 판매
      _Section(title: '① 판매 현황', subtitle: '결제 완료(PAID) 주문 기준', children: [
        _CardGrid(cards: [
          _StatCard('총 누적 매출', _won(_n(summary['total_amount']))),
          _StatCard('판매 건수', '${_n(summary['total_orders']).round()}'),
          _StatCard('일평균 매출', _won(_n(summary['avg_amount']))),
          _StatCard('최다 판매 카테고리', '${sales['top_category'] ?? '-'}'),
        ]),
        _Label('카테고리별 매출'),
        _HBars(labels: _strs(sales['categories']), values: _nums(sales['amounts']), color: cs.primary, format: _won),
        _Label('일자별 매출'),
        _GroupedBars(labels: _strs(sales['daily_dates']), series: [('매출', cs.primary, _nums(sales['daily_amounts']))]),
      ]),
      // ② 커뮤니티
      _Section(title: '② 커뮤니티 활동', subtitle: '최근 14일, 팔로우는 전체 누적', children: [
        _CardGrid(cards: [
          _StatCard('새 게시글', '${_n(cSummary['posts']).round()}'),
          _StatCard('좋아요', '${_n(cSummary['likes']).round()}'),
          _StatCard('댓글', '${_n(cSummary['comments']).round()}'),
          _StatCard('팔로우(누적)', '${_n(cSummary['follows']).round()}'),
        ]),
        _Label('일자별 활동'),
        _GroupedBars(labels: _strs(community['activity_dates']), series: [
          ('게시글', cs.primary, _nums(community['activity_posts'])),
          ('좋아요', Colors.pink.shade300, _nums(community['activity_likes'])),
          ('댓글', Colors.purple.shade300, _nums(community['activity_comments'])),
        ]),
        _Label('인기 해시태그'),
        _HBars(labels: _strs(community['top_tags']).map((t) => '#$t').toList(), values: _nums(community['top_tag_counts']), color: cs.tertiary),
      ]),
      // ③ 챗봇
      _Section(title: '③ 상담 챗봇', subtitle: '최근 30일', children: [
        _CardGrid(cards: [
          _StatCard('전체 질문', '${_n(chatbot['chatbot_total']).round()}'),
          _StatCard('응답률', '${_n(chatbot['answer_rate']).toStringAsFixed(_n(chatbot['answer_rate']) % 1 == 0 ? 0 : 1)}%'),
        ]),
        _Label('응답 방식'),
        _HBars(labels: _strs(chatbot['chatbot_sources']), values: _nums(chatbot['chatbot_source_counts']), color: cs.secondary),
        _Label('많이 찾은 질문'),
        _PairList(items: _pairs(chatbot['top_faqs']), empty: '데이터 없음'),
        _Label('답하지 못한 질문'),
        _PairList(items: _pairs(chatbot['unanswered']), empty: '모든 질문에 답했습니다 👍'),
      ]),
      // ④ 운영 지표
      _Section(title: '④ 운영 지표 추이', subtitle: 'Spring Boot → Django 동기화 값', children: [
        _GroupedBars(labels: _strs(ops['ops_dates']).map((s) => s.length >= 10 ? s.substring(5) : s).toList(), series: [
          ('신규 가입', cs.primary, _nums(ops['ops_users'])),
          ('결제', Colors.green.shade400, _nums(ops['ops_orders'])),
          ('챗봇 질문', Colors.orange.shade400, _nums(ops['ops_questions'])),
        ]),
      ]),
    ];
  }
}

// ---------- 데이터 변환 ----------
Map _m(dynamic v) => v is Map ? v : const {};
num _n(dynamic v) => v is num ? v : num.tryParse('$v') ?? 0;
List<num> _nums(dynamic v) => v is List ? v.map(_n).toList() : <num>[];
List<String> _strs(dynamic v) => v is List ? v.map((e) => '$e').toList() : <String>[];

/// top_faqs / unanswered 는 [이름, 개수] 배열이나 {label, count} 객체, 혹은 문자열일 수 있어서 모두 받아 준다
List<(String, num?)> _pairs(dynamic v) {
  if (v is! List) return [];
  return v.map<(String, num?)>((e) {
    if (e is Map) {
      final dynamic label = e['label'] ?? e['name'] ?? e['question'] ?? e['faq'] ?? (e.isNotEmpty ? e.values.first : '');
      final dynamic c = e['count'] ?? e['cnt'] ?? e['value'];
      return ('$label', c is num ? c : null);
    }
    if (e is List && e.isNotEmpty) return ('${e[0]}', e.length > 1 && e[1] is num ? e[1] as num : null);
    return ('$e', null);
  }).toList();
}

String _won(num v) {
  final String s = v.round().abs().toString();
  final StringBuffer b = StringBuffer(v < 0 ? '-' : '');
  for (int i = 0; i < s.length; i++) {
    if (i > 0 && (s.length - i) % 3 == 0) b.write(',');
    b.write(s[i]);
  }
  return '$b원';
}

// ---------- 화면 조각 ----------
class _Section extends StatelessWidget {
  final String title;
  final String subtitle;
  final List<Widget> children;
  const _Section({required this.title, required this.subtitle, required this.children});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 20),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.primary)),
        Text(subtitle, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 10),
        ...children,
      ]),
    );
  }
}

class _Label extends StatelessWidget {
  final String text;
  const _Label(this.text);

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 14, bottom: 6),
        child: Text(text, style: const TextStyle(fontWeight: FontWeight.w600)),
      );
}

class _StatCard {
  final String label;
  final String value;
  const _StatCard(this.label, this.value);
}

class _CardGrid extends StatelessWidget {
  final List<_StatCard> cards;
  const _CardGrid({required this.cards});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, c) {
      final double w = (c.maxWidth - 8) / 2;
      return Wrap(spacing: 8, runSpacing: 8, children: [
        for (final card in cards)
          SizedBox(
            width: w,
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(card.label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                const SizedBox(height: 4),
                Text(card.value, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
              ]),
            ),
          ),
      ]);
    });
  }
}

class _Empty extends StatelessWidget {
  final String text;
  const _Empty([this.text = '아직 데이터가 없습니다.']);

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Text(text, style: const TextStyle(color: Colors.grey)),
      );
}

/// 가로 막대 (카테고리별 매출, 해시태그, 응답 방식)
class _HBars extends StatelessWidget {
  final List<String> labels;
  final List<num> values;
  final Color color;
  final String Function(num)? format;
  const _HBars({required this.labels, required this.values, required this.color, this.format});

  @override
  Widget build(BuildContext context) {
    final int n = labels.length < values.length ? labels.length : values.length;
    if (n == 0 || values.every((v) => v == 0)) return const _Empty();
    final num max = values.take(n).reduce((a, b) => a > b ? a : b);
    return Column(children: [
      for (int i = 0; i < n; i++)
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 3),
          child: Row(children: [
            SizedBox(width: 92, child: Text(labels[i], maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13))),
            Expanded(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: Stack(children: [
                  Container(height: 16, color: color.withValues(alpha: 0.12)),
                  FractionallySizedBox(alignment: Alignment.centerLeft, widthFactor: max == 0 ? 0 : (values[i] / max).clamp(0, 1).toDouble(), child: Container(height: 16, color: color)),
                ]),
              ),
            ),
            const SizedBox(width: 8),
            Text(format != null ? format!(values[i]) : '${values[i].round()}', style: const TextStyle(fontSize: 12)),
          ]),
        ),
    ]);
  }
}

/// 날짜별 세로 막대 (계열이 여러 개면 날짜마다 나란히)
class _GroupedBars extends StatelessWidget {
  final List<String> labels;
  final List<(String, Color, List<num>)> series;
  const _GroupedBars({required this.labels, required this.series});

  @override
  Widget build(BuildContext context) {
    final int n = labels.length;
    final List<num> all = [for (final s in series) ...s.$3];
    if (n == 0 || all.isEmpty || all.every((v) => v == 0)) return const _Empty();
    final num max = all.reduce((a, b) => a > b ? a : b);
    const double h = 110;
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      if (series.length > 1)
        Wrap(spacing: 12, children: [
          for (final s in series)
            Row(mainAxisSize: MainAxisSize.min, children: [
              Container(width: 10, height: 10, color: s.$2),
              const SizedBox(width: 4),
              Text(s.$1, style: const TextStyle(fontSize: 12)),
            ]),
        ]),
      const SizedBox(height: 6),
      SizedBox(
        height: h,
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          for (int i = 0; i < n; i++)
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 1.5),
                child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                  for (final s in series)
                    Expanded(
                      child: Container(
                        height: i < s.$3.length && max > 0 ? (h * s.$3[i] / max).clamp(1, h).toDouble() : 1,
                        decoration: BoxDecoration(color: s.$2, borderRadius: const BorderRadius.vertical(top: Radius.circular(2))),
                      ),
                    ),
                ]),
              ),
            ),
        ]),
      ),
      const SizedBox(height: 4),
      Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text(labels.first, style: const TextStyle(fontSize: 11, color: Colors.grey)),
        if (n > 1) Text(labels.last, style: const TextStyle(fontSize: 11, color: Colors.grey)),
      ]),
    ]);
  }
}

class _PairList extends StatelessWidget {
  final List<(String, num?)> items;
  final String empty;
  const _PairList({required this.items, required this.empty});

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) return _Empty(empty);
    return Column(children: [
      for (final it in items.take(10))
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 3),
          child: Row(children: [
            const Text('• '),
            Expanded(child: Text(it.$1, maxLines: 2, overflow: TextOverflow.ellipsis)),
            if (it.$2 != null) Text('${it.$2!.round()}회', style: const TextStyle(color: Colors.grey)),
          ]),
        ),
    ]);
  }
}
