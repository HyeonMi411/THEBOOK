import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../../shared/main_shell.dart';
import '../../auth/data/auth_provider.dart';
import '../../books/data/book_provider.dart';
import '../../books/presentation/book_list_page.dart';
import '../../books/presentation/external_search_page.dart';
import '../../books/presentation/national_library_page.dart';
import '../../chatbot/presentation/chatbot_page.dart';
import '../../map/presentation/store_map_page.dart';
import '../../notices/notice_pages.dart';
import '../data/home_provider.dart';

/// 홈 - 날씨(기상청) · 바로가기 · 베스트셀러 · 공지사항 · 오늘의 책 소식(RSS 크롤링)
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final String nick = ref.watch(authProvider).nickname;
    return AppLayout(
      title: 'BookStore',
      child: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(weatherProvider);
          ref.invalidate(bestsellerProvider);
          ref.invalidate(noticePageProvider(3));
          ref.invalidate(bookNewsProvider);
        },
        child: ListView(padding: const EdgeInsets.only(bottom: 24), children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
            child: Text(nick.isEmpty ? '오늘은 어떤 책을 만나볼까요?' : '$nick님, 오늘은 어떤 책을 만나볼까요?',
                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          ),
          const _WeatherCard(),
          _shortcuts(context),
          _section(context, '베스트셀러 TOP 10', onMore: () => MainShell.tabIndex.value = 1),
          const _Bestsellers(),
          _section(context, '공지사항', onMore: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NoticeListPage()))),
          const _NoticePreview(),
          _section(context, '오늘의 책 소식'),
          const _BookNews(),
        ]),
      ),
    );
  }

  Widget _shortcuts(BuildContext context) {
    Widget item(IconData icon, String label, Widget page) => Expanded(
          child: InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => page)),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 10),
              child: Column(children: [
                CircleAvatar(radius: 24, backgroundColor: Colors.blue.shade50, child: Icon(icon, color: Colors.blue.shade700)),
                const SizedBox(height: 6),
                Text(label, style: const TextStyle(fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
              ]),
            ),
          ),
        );
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(children: [
        item(Icons.map_outlined, '매장 지도', const StoreMapPage()),
        item(Icons.travel_explore, '외부 도서검색', const ExternalSearchPage()),
        item(Icons.account_balance_outlined, '국립중앙도서관', const NationalLibraryPage()),
        item(Icons.campaign_outlined, '공지사항', const NoticeListPage()),
        item(Icons.support_agent, '상담 챗봇', const ChatbotPage()),
      ]),
    );
  }

  Widget _section(BuildContext context, String title, {VoidCallback? onMore}) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 18, 8, 6),
      child: Row(children: [
        Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
        const Spacer(),
        if (onMore != null) TextButton(onPressed: onMore, child: const Text('더보기')),
      ]),
    );
  }
}

class _WeatherCard extends ConsumerWidget {
  const _WeatherCard();

  IconData _icon(int pty) {
    if (pty == 1 || pty == 5) return Icons.umbrella;
    if (pty == 2 || pty == 6) return Icons.cloudy_snowing;
    if (pty == 3 || pty == 7) return Icons.ac_unit;
    return Icons.wb_sunny_outlined;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<Map<String, dynamic>> w = ref.watch(weatherProvider);
    return Card(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 4),
      color: Colors.blue.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: w.when(
          loading: () => const SizedBox(height: 64, child: Center(child: CircularProgressIndicator())),
          error: (_, _) => const Text('날씨 정보를 불러오지 못했어요.'),
          data: (d) {
            if (d['available'] != true) {
              return Row(children: [
                const Icon(Icons.cloud_off, color: Colors.grey),
                const SizedBox(width: 10),
                Expanded(child: Text(d['message']?.toString() ?? '날씨 정보를 사용할 수 없습니다.')),
              ]);
            }
            final int pty = asInt(d['precipitationType']);
            return Row(children: [
              Icon(_icon(pty), size: 44, color: Colors.orange.shade700),
              const SizedBox(width: 14),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('${d['temperature']}℃ · ${d['precipitationText']}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  Text('습도 ${d['humidity'] ?? '-'}% · 바람 ${d['windSpeed'] ?? '-'}m/s · ${d['usedMyLocation'] == true ? '내 위치' : '서울'}',
                      style: const TextStyle(color: Colors.black54, fontSize: 12)),
                  const SizedBox(height: 4),
                  Text(d['readingTip']?.toString() ?? '', style: const TextStyle(fontSize: 13)),
                ]),
              ),
            ]);
          },
        ),
      ),
    );
  }
}

class _Bestsellers extends ConsumerWidget {
  const _Bestsellers();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> best = ref.watch(bestsellerProvider);
    return SizedBox(
      height: 250,
      child: best.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => const EmptyView(message: '베스트셀러를 불러오지 못했어요.'),
        data: (list) => list.isEmpty
            ? const EmptyView(message: '아직 판매 기록이 없어요.', icon: Icons.emoji_events_outlined)
            : ListView.builder(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(horizontal: 10),
                itemCount: list.length,
                itemBuilder: (_, i) => SizedBox(
                  width: 140,
                  child: BookCard(
                    book: Map<String, dynamic>.from(list[i]['book'] as Map),
                    badge: '${list[i]['rank']}위',
                  ),
                ),
              ),
      ),
    );
  }
}

class _NoticePreview extends ConsumerWidget {
  const _NoticePreview();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> n = ref.watch(noticePageProvider(3));
    return n.when(
      loading: () => const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
      error: (_, _) => const Padding(padding: EdgeInsets.all(16), child: Text('공지사항을 불러오지 못했어요.')),
      data: (list) => list.isEmpty
          ? const Padding(padding: EdgeInsets.symmetric(horizontal: 16), child: Text('등록된 공지가 없습니다.', style: TextStyle(color: Colors.grey)))
          : Column(children: [for (final Map<String, dynamic> x in list) NoticeTile(notice: x)]),
    );
  }
}

class _BookNews extends ConsumerWidget {
  const _BookNews();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> news = ref.watch(bookNewsProvider);
    return news.when(
      loading: () => const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
      error: (_, _) => const Padding(padding: EdgeInsets.all(16), child: Text('소식을 불러오지 못했어요.')),
      data: (list) => list.isEmpty
          ? const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: Text('아직 수집된 소식이 없어요. (서버 시작 후 몇 초 뒤 수집됩니다)', style: TextStyle(color: Colors.grey)))
          : Column(children: [
              for (final Map<String, dynamic> a in list.take(8))
                ListTile(
                  dense: true,
                  leading: const Icon(Icons.newspaper_outlined),
                  title: Text(a['title']?.toString() ?? '', maxLines: 2, overflow: TextOverflow.ellipsis),
                  subtitle: Text('${a['source'] ?? ''} · ${a['publishedAt'] ?? ''}'),
                  onTap: () => launchUrl(Uri.parse(a['link'].toString()), mode: LaunchMode.externalApplication),
                ),
            ]),
    );
  }
}
