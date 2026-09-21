import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/notice_provider.dart';

class NoticeDetailPage extends ConsumerStatefulWidget {
  final int noticeId;
  const NoticeDetailPage({super.key, required this.noticeId});

  @override
  ConsumerState<NoticeDetailPage> createState() => _NoticeDetailPageState();
}

class _NoticeDetailPageState extends ConsumerState<NoticeDetailPage> {
  Map<String, dynamic>? _notice;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final data = await ref.read(noticeProvider.notifier).fetchNoticeDetail(widget.noticeId);
    setState(() {
      _notice = data;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Scaffold(body: Center(child: CircularProgressIndicator()));
    if (_notice == null) return const Scaffold(body: Center(child: Text('공지사항을 불러올 수 없습니다.')));
    final notice = _notice!;

    return Scaffold(
      appBar: AppBar(title: Text(notice['title'] ?? '공지사항')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(notice['title'] ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text('조회수 ${notice['hit'] ?? 0}', style: const TextStyle(color: Colors.grey)),
            const Divider(height: 32),
            Text(notice['content'] ?? ''),
          ],
        ),
      ),
    );
  }
}
