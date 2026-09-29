import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_client.dart';
import '../../core/utils/format.dart';
import '../../shared/app_layout.dart';
import '../auth/data/auth_provider.dart';
import '../home/data/home_provider.dart';
import 'notice_form_page.dart';

class NoticeListPage extends ConsumerWidget {
  const NoticeListPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> notices = ref.watch(noticePageProvider(50));
    final bool isAdmin = ref.watch(authProvider).isAdmin;
    return Scaffold(
      appBar: AppBar(title: const Text('공지사항')),
      floatingActionButton: isAdmin
          ? FloatingActionButton.extended(
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const NoticeFormPage())),
              icon: const Icon(Icons.edit),
              label: const Text('공지 작성'),
            )
          : null,
      body: notices.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e), onRetry: () => ref.invalidate(noticePageProvider(50))),
        data: (list) => list.isEmpty
            ? const EmptyView(message: '등록된 공지사항이 없습니다.', icon: Icons.campaign_outlined)
            : ListView.separated(
                itemCount: list.length,
                separatorBuilder: (_, _) => const Divider(height: 1),
                itemBuilder: (context, i) => NoticeTile(notice: list[i]),
              ),
      ),
    );
  }
}

class NoticeTile extends StatelessWidget {
  final Map<String, dynamic> notice;
  const NoticeTile({super.key, required this.notice});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.campaign_outlined),
      title: Text(notice['btitle']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis),
      subtitle: Text('${shortDate(notice['createdAt'])} · 조회 ${notice['bhit'] ?? 0}'),
      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => NoticeDetailPage(id: asInt(notice['id'])))),
    );
  }
}

class NoticeDetailPage extends ConsumerWidget {
  final int id;
  const NoticeDetailPage({super.key, required this.id});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<Map<String, dynamic>> n = ref.watch(noticeDetailProvider(id));
    final bool isAdmin = ref.watch(authProvider).isAdmin;
    return Scaffold(
      appBar: AppBar(
        title: const Text('공지사항'),
        actions: [
          if (isAdmin)
            n.maybeWhen(
              data: (d) => PopupMenuButton<String>(
                icon: const Icon(Icons.admin_panel_settings_outlined),
                onSelected: (v) => v == 'edit'
                    ? Navigator.push(context, MaterialPageRoute(builder: (_) => NoticeFormPage(editing: d)))
                    : _delete(context, ref),
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'edit', child: Text('수정')),
                  PopupMenuItem(value: 'delete', child: Text('삭제', style: TextStyle(color: Colors.red))),
                ],
              ),
              orElse: () => const SizedBox.shrink(),
            ),
        ],
      ),
      body: n.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e)),
        data: (d) => ListView(padding: const EdgeInsets.all(16), children: [
          Text(d['btitle']?.toString() ?? '', style: const TextStyle(fontSize: 19, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text('${d['userNickname'] ?? '관리자'} · ${shortDateTime(d['createdAt'])} · 조회 ${d['bhit'] ?? 0}',
              style: const TextStyle(color: Colors.grey)),
          const Divider(height: 28),
          if ((d['bfile']?.toString() ?? '').isNotEmpty)
            Padding(padding: const EdgeInsets.only(bottom: 12), child: NetImage(imageUrl(d['bfile']), fit: BoxFit.fitWidth, fallbackIcon: Icons.image)),
          Text(d['bcontent']?.toString() ?? '', style: const TextStyle(height: 1.7)),
        ]),
      ),
    );
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: const Text('이 공지를 삭제할까요?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
          FilledButton(style: FilledButton.styleFrom(backgroundColor: Colors.red), onPressed: () => Navigator.pop(ctx, true), child: const Text('삭제')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await DioClient.instance.delete('/api/notices/$id');
      ref.invalidate(noticePageProvider);
      if (context.mounted) Navigator.pop(context);
    } catch (e) {
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    }
  }
}
