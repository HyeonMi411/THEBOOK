import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/board_provider.dart';
import '../../auth/data/auth_provider.dart';
import '../../../core/network/api_client.dart';

class PostDetailPage extends ConsumerWidget {  // final Map<String, dynamic> post;

  const PostDetailPage({super.key, required this.post});

  // 이미지 URL 정합성 맞추기 헬퍼 메서드
  String _resolveImageUrl(String url) {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    final String serverBaseUrl = ApiClient.getBaseUrl();
    final cleanBase = serverBaseUrl.endsWith('/')
        ? serverBaseUrl.substring(0, serverBaseUrl.length - 1)
        : serverBaseUrl;
    final cleanUrl = url.startsWith('/') ? url : '/$url';
    return '$cleanBase$cleanUrl';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final postId = post['id'];

    // [수정 포인트]: boardState가 BoardState 객체이므로 내부 리스트(posts, boardList, items)를 감지하여 순회
    final boardState = ref.watch(boardProvider);
    Map<String, dynamic> currentPost = post; // 찾지 못할 경우 기본 post 사용

    final dynamic rawPosts = (boardState as dynamic).posts ??
        (boardState as dynamic).boardList ??
        (boardState as dynamic).items;

    if (rawPosts is List) {
      for (final item in rawPosts) {
        if (item is Map && item['id'] == postId) {
          currentPost = Map<String, dynamic>.from(item);
          break;
        }
      }
    }

    // 널 안전성(Null Safety) 및 타입 안전 추출
    final String postNickname = (currentPost['authorNickname'] ??
            currentPost['userNickname'] ??
            currentPost['nickname'] ??
            currentPost['writerNickname'] ??
            currentPost['user']?['nickname'] ??
            currentPost['writer']?['nickname'] ??
            '')
        .toString();

    final String content = (currentPost['content'] ?? '').toString();
    final List<dynamic> hashtags =
        currentPost['hashtags'] is List ? currentPost['hashtags'] : [];
    final List<dynamic> imageUrls =
        currentPost['imageUrls'] is List ? currentPost['imageUrls'] : [];
    final String createdAt = (currentPost['createdAt'] ?? '').toString();

    // 현재 로그인한 사용자 정보 감지
    final authState = ref.watch(authProvider);
    final currentUser = authState.user;

    // 작성자 본인 판별 로직
    bool isMyPost = false;

    if (currentUser != null) {
      final postUserId = currentPost['authorId'] ??
          currentPost['userId'] ??
          currentPost['user_id'] ??
          currentPost['memberId'] ??
          currentPost['writerId'];

      final currentUserId = currentUser['id'] ??
          currentUser['userId'] ??
          currentUser['memberId'] ??
          currentUser['authorId'];

      final currentNickname = currentUser['nickname'] ??
          currentUser['authorNickname'] ??
          currentUser['userNickname'] ??
          currentUser['name'] ??
          '';

      if (postUserId != null && currentUserId != null) {
        isMyPost =
            postUserId.toString().trim() == currentUserId.toString().trim();
      }

      if (!isMyPost && postNickname.isNotEmpty && currentNickname.isNotEmpty) {
        isMyPost = postNickname.trim() == currentNickname.trim();
      }
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('게시글 상세보기'),
        actions: [
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            Text(
              '작성자: ${postNickname.isEmpty ? '익명' : postNickname}',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.blue,
              ),
            ),
            const SizedBox(height: 4),
            if (createdAt.isNotEmpty)
              Text(
                '작성일: $createdAt',
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            const Divider(height: 24),
            Text(
              content,
              style: const TextStyle(fontSize: 18),
            ),
            const SizedBox(height: 16),
            if (hashtags.isNotEmpty)
              Wrap(
                spacing: 6.0,
                children: hashtags
                    .map(
                      (tag) => Text(
                        tag.toString().startsWith('#')
                            ? tag.toString()
                            : '#$tag',
                        style: const TextStyle(
                          color: Colors.indigo,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                    .toList(),
              ),
            const SizedBox(height: 16),
            if (imageUrls.isNotEmpty)
              ...imageUrls.map((url) {
                final resolvedUrl = _resolveImageUrl(url.toString());
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12.0),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8.0),
                    child: Image.network(
                      Uri.encodeFull(resolvedUrl),
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        padding: const EdgeInsets.all(16),
                        color: Colors.grey[200],
                        child: Text(
                          '이미지를 불러올 수 없습니다.\n($resolvedUrl)',
                          textAlign: TextAlign.center,
                          style:
                              const TextStyle(color: Colors.red, fontSize: 11),
                        ),
                      ),
                    ),
                  ),
                );
              }),
          ],
        ),
      ),
    );
  }
}
