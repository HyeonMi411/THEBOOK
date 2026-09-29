import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/board_provider.dart';
import 'post_detail_page.dart';
import 'user_profile_page.dart';

/// 게시글 카드 - 목록/프로필/좋아요한 글에서 공통 사용
/// 좋아요·리트윗은 카드 안에서 바로 반영하고(낙관적 갱신 대신 서버 응답값 사용), 상세에서 돌아오면 [onChanged] 로 목록 새로고침
class PostCard extends ConsumerStatefulWidget {
  final Map<String, dynamic> post;
  final void Function(String tag)? onTagTap;
  final VoidCallback? onChanged;

  const PostCard({super.key, required this.post, this.onTagTap, this.onChanged});

  @override
  ConsumerState<PostCard> createState() => _PostCardState();
}

class _PostCardState extends ConsumerState<PostCard> {
  late Map<String, dynamic> p;

  @override
  void initState() {
    super.initState();
    p = Map<String, dynamic>.from(widget.post);
  }

  @override
  void didUpdateWidget(covariant PostCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.post != widget.post) p = Map<String, dynamic>.from(widget.post);
  }

  Future<void> _like() async {
    if (!requireLogin(context, ref)) return;
    try {
      final Map<String, dynamic> r = await BoardApi.toggleLike(asInt(p['id']));
      setState(() {
        p['likedByMe'] = r['liked'];
        p['likeCount'] = r['likeCount'];
      });
    } catch (e) {
      _toast(errorMessage(e));
    }
  }

  Future<void> _retweet() async {
    if (!requireLogin(context, ref)) return;
    try {
      final Map<String, dynamic> r = await BoardApi.toggleRetweet(asInt(p['id']));
      setState(() {
        p['retweetedByMe'] = r['retweeted'];
        p['retweetCount'] = r['retweetCount'];
      });
      _toast(r['retweeted'] == true ? '내 프로필과 팔로워 피드에 공유했어요.' : '리트윗을 취소했어요.');
    } catch (e) {
      _toast(errorMessage(e));
    }
  }

  void _toast(String m) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));
  }

  @override
  Widget build(BuildContext context) {
    final int? myId = ref.watch(authProvider).userId;
    final bool mine = myId != null && myId == asInt(p['userId']);
    final List<dynamic> images = p['imageUrls'] as List? ?? [];
    final List<dynamic> tags = p['hashtags'] as List? ?? [];
    final String profile = p['userProfileImage']?.toString() ?? '';

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      child: InkWell(
        onTap: () async {
          await Navigator.push(context, MaterialPageRoute(builder: (_) => PostDetailPage(postId: asInt(p['id']))));
          widget.onChanged?.call();
        },
        child: Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 4),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            if (p['retweetedBy'] != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Row(children: [
                  const Icon(Icons.repeat, size: 14, color: Colors.green),
                  const SizedBox(width: 4),
                  Text('${p['retweetedBy']}님이 리트윗', style: const TextStyle(fontSize: 12, color: Colors.green)),
                ]),
              ),
            InkWell(
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => UserProfilePage(userId: asInt(p['userId'])))),
              child: Row(children: [
                CircleAvatar(
                  radius: 16,
                  backgroundImage: profile.isNotEmpty ? NetworkImage(imageUrl(profile)) : null,
                  child: profile.isEmpty ? const Icon(Icons.person, size: 18) : null,
                ),
                const SizedBox(width: 8),
                Text(p['userNickname']?.toString() ?? '알 수 없음', style: const TextStyle(fontWeight: FontWeight.bold)),
                const Spacer(),
                Text(shortDateTime(p['createdAt']), style: const TextStyle(fontSize: 12, color: Colors.grey)),
              ]),
            ),
            const SizedBox(height: 8),
            Text(p['content']?.toString() ?? '', maxLines: 6, overflow: TextOverflow.ellipsis),
            if (images.isNotEmpty) ...[
              const SizedBox(height: 8),
              SizedBox(
                height: 160,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: images.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 6),
                  itemBuilder: (_, i) => ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: NetImage(imageUrl(images[i]), width: 160, height: 160, fallbackIcon: Icons.image),
                  ),
                ),
              ),
            ],
            if (tags.isNotEmpty)
              Wrap(spacing: 6, children: [
                for (final dynamic t in tags)
                  ActionChip(
                    label: Text('#$t', style: TextStyle(color: Colors.blue.shade700)),
                    visualDensity: VisualDensity.compact,
                    onPressed: widget.onTagTap == null ? null : () => widget.onTagTap!(t.toString()),
                  ),
              ]),
            Row(children: [
              TextButton.icon(
                onPressed: _like,
                icon: Icon(p['likedByMe'] == true ? Icons.favorite : Icons.favorite_border, color: Colors.red, size: 20),
                label: Text('${p['likeCount'] ?? 0}'),
              ),
              TextButton.icon(
                onPressed: null,
                icon: const Icon(Icons.chat_bubble_outline, size: 20),
                label: Text('${p['commentCount'] ?? 0}'),
              ),
              TextButton.icon(
                onPressed: mine ? null : _retweet,
                icon: Icon(Icons.repeat, size: 20, color: p['retweetedByMe'] == true ? Colors.green : null),
                label: Text('${p['retweetCount'] ?? 0}'),
              ),
            ]),
          ]),
        ),
      ),
    );
  }
}
