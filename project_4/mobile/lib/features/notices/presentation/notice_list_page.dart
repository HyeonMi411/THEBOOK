import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../shared/components/app_layout.dart';
import '../data/notice_provider.dart';
import 'notice_detail_page.dart';

class NoticeListPage extends ConsumerStatefulWidget {
  const NoticeListPage({super.key});

  @override
  ConsumerState<NoticeListPage> createState() => _NoticeListPageState();
}

class _NoticeListPageState extends ConsumerState<NoticeListPage> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(noticeProvider.notifier).fetchNotices());
  }

  @override
  Widget build(BuildContext context) {
    final noticeState = ref.watch(noticeProvider);

    return AppLayout(
      child: noticeState.loading && noticeState.notices.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : noticeState.notices.isEmpty
              ? const Center(child: Text('등록된 공지사항이 없습니다.'))
              : RefreshIndicator(
                  onRefresh: () => ref.read(noticeProvider.notifier).fetchNotices(),
                  child: ListView.separated(
                    itemCount: noticeState.notices.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (context, index) {
                      final notice = noticeState.notices[index];
                      return ListTile(
                        title: Text(notice['title'] ?? ''),
                        subtitle: Text('조회 ${notice['hit'] ?? 0}'),
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(builder: (_) => NoticeDetailPage(noticeId: notice['id'])),
                          );
                        },
                      );
                    },
                  ),
                ),
    );
  }
}
