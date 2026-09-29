import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/board_provider.dart';
import 'post_comments_section.dart';
import 'post_write_page.dart';
import 'user_profile_page.dart';

class PostDetailPage extends ConsumerWidget {
  final int postId;
  const PostDetailPage({super.key, required this.postId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<Map<String, dynamic>> post = ref.watch(postDetailProvider(postId));
    final int? myId = ref.watch(authProvider).userId;
    return Scaffold(
      appBar: AppBar(
        title: const Text('게시글'),
        actions: [
          post.maybeWhen(
            data: (p) => myId != null && myId == asInt(p['userId'])
                ? PopupMenuButton<String>(
                    onSelected: (v) => v == 'edit' ? _edit(context, ref, p) : _delete(context, ref),
                    itemBuilder: (_) => const [
                      PopupMenuItem(value: 'edit', child: Text('수정')),
                      PopupMenuItem(value: 'delete', child: Text('삭제')),
                    ],
                  )
                : const SizedBox.shrink(),
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      ),
      body: post.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e)),
        data: (p) => _body(context, ref, p),
      ),
    );
  }

  Widget _body(BuildContext context, WidgetRef ref, Map<String, dynamic> p) {
    final List<dynamic> images = p['imageUrls'] as List? ?? [];
    final List<dynamic> tags = p['hashtags'] as List? ?? [];
    final String profile = p['userProfileImage']?.toString() ?? '';
    final bool mine = ref.read(authProvider).userId == asInt(p['userId']);

    Future<void> act(Future<Map<String, dynamic>> Function() call) async {
      if (!requireLogin(context, ref)) return;
      try {
        await call();
        ref.invalidate(postDetailProvider(postId));
      } catch (e) {
        if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
      }
    }

    return ListView(padding: const EdgeInsets.all(16), children: [
      ListTile(
        contentPadding: EdgeInsets.zero,
        leading: CircleAvatar(
          backgroundImage: profile.isNotEmpty ? NetworkImage(imageUrl(profile)) : null,
          child: profile.isEmpty ? const Icon(Icons.person) : null,
        ),
        title: Text(p['userNickname']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(shortDateTime(p['createdAt'])),
        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => UserProfilePage(userId: asInt(p['userId'])))),
      ),
      Text(p['content']?.toString() ?? '', style: const TextStyle(fontSize: 16, height: 1.6)),
      for (final dynamic img in images)
        Padding(
          padding: const EdgeInsets.only(top: 10),
          child: ClipRRect(borderRadius: BorderRadius.circular(8), child: NetImage(imageUrl(img), fit: BoxFit.fitWidth, fallbackIcon: Icons.image)),
        ),
      if (tags.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(top: 10),
          child: Wrap(spacing: 6, children: [for (final dynamic t in tags) Chip(label: Text('#$t'))]),
        ),
      Row(children: [
        TextButton.icon(
          onPressed: () => act(() => BoardApi.toggleLike(postId)),
          icon: Icon(p['likedByMe'] == true ? Icons.favorite : Icons.favorite_border, color: Colors.red),
          label: Text('좋아요 ${p['likeCount'] ?? 0}'),
        ),
        TextButton.icon(
          onPressed: mine ? null : () => act(() => BoardApi.toggleRetweet(postId)),
          icon: Icon(Icons.repeat, color: p['retweetedByMe'] == true ? Colors.green : null),
          label: Text('리트윗 ${p['retweetCount'] ?? 0}'),
        ),
      ]),
      const Divider(),
      PostCommentsSection(postId: postId, onChanged: () => ref.invalidate(postDetailProvider(postId))),
    ]);
  }

  Future<void> _edit(BuildContext context, WidgetRef ref, Map<String, dynamic> p) async {
    final bool? ok = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => PostWritePage(editing: p)));
    if (ok == true) ref.invalidate(postDetailProvider(postId));
  }

  Future<void> _delete(BuildContext context, WidgetRef ref) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: const Text('이 글을 삭제할까요?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('삭제')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await BoardApi.delete(postId);
      ref.invalidate(feedProvider);
      if (context.mounted) Navigator.pop(context);
    } catch (e) {
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    }
  }
}
