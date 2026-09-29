import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../data/board_provider.dart';
import 'follow_list_page.dart';
import 'post_card.dart';

/// 사용자 프로필 - 글 수 / 팔로워 / 팔로잉 + 팔로우 버튼 + 쓴 글·리트윗한 글
class UserProfilePage extends ConsumerWidget {
  final int userId;
  const UserProfilePage({super.key, required this.userId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<Map<String, dynamic>> profile = ref.watch(userProfileProvider(userId));
    final AsyncValue<List<Map<String, dynamic>>> posts = ref.watch(userPostsProvider(userId));
    return Scaffold(
      appBar: AppBar(title: Text(profile.maybeWhen(data: (p) => p['nickname']?.toString() ?? '프로필', orElse: () => '프로필'))),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(userProfileProvider(userId));
          ref.invalidate(userPostsProvider(userId));
        },
        child: ListView(children: [
          profile.when(
            loading: () => const Padding(padding: EdgeInsets.all(40), child: Center(child: CircularProgressIndicator())),
            error: (e, _) => EmptyView(message: errorMessage(e)),
            data: (p) => _header(context, ref, p),
          ),
          const Divider(),
          ...posts.when<List<Widget>>(
            loading: () => [const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator()))],
            error: (e, _) => [EmptyView(message: errorMessage(e))],
            data: (list) => list.isEmpty
                ? [const EmptyView(message: '아직 작성하거나 리트윗한 글이 없어요.', icon: Icons.article_outlined)]
                : [
                    for (final Map<String, dynamic> post in list)
                      PostCard(
                        key: ValueKey('${post['id']}-${post['retweetedBy']}'),
                        post: post,
                        onChanged: () => ref.invalidate(userPostsProvider(userId)),
                      ),
                  ],
          ),
          const SizedBox(height: 24),
        ]),
      ),
    );
  }

  Widget _header(BuildContext context, WidgetRef ref, Map<String, dynamic> p) {
    final String img = p['profileImage']?.toString() ?? '';
    final bool me = p['me'] == true;
    final bool following = p['followedByMe'] == true;
    Widget stat(String label, dynamic value, VoidCallback? onTap) => InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Column(children: [
              Text('${value ?? 0}', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              Text(label, style: const TextStyle(color: Colors.grey)),
            ]),
          ),
        );
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(children: [
        CircleAvatar(
          radius: 40,
          backgroundImage: img.isNotEmpty ? NetworkImage(imageUrl(img)) : null,
          child: img.isEmpty ? const Icon(Icons.person, size: 40) : null,
        ),
        const SizedBox(height: 8),
        Text(p['nickname']?.toString() ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
          stat('게시글', p['postCount'], null),
          stat('팔로워', p['followerCount'],
              () => Navigator.push(context, MaterialPageRoute(builder: (_) => FollowListPage(userId: userId, type: 'followers')))),
          stat('팔로잉', p['followingCount'],
              () => Navigator.push(context, MaterialPageRoute(builder: (_) => FollowListPage(userId: userId, type: 'followings')))),
        ]),
        if (!me)
          SizedBox(
            width: 200,
            child: following
                ? OutlinedButton(onPressed: () => _toggle(context, ref), child: const Text('팔로잉 ✓'))
                : FilledButton(onPressed: () => _toggle(context, ref), child: const Text('팔로우')),
          ),
      ]),
    );
  }

  Future<void> _toggle(BuildContext context, WidgetRef ref) async {
    if (!requireLogin(context, ref)) return;
    try {
      await BoardApi.toggleFollow(userId);
      ref.invalidate(userProfileProvider(userId));
      ref.invalidate(feedProvider);
    } catch (e) {
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    }
  }
}
