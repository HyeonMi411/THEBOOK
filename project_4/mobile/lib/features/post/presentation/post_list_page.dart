import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/board_provider.dart';
import 'post_card.dart';
import 'post_write_page.dart';

/// 커뮤니티 - 전체 / 팔로잉 피드 + 해시태그 필터 + 글쓰기
class PostListPage extends ConsumerStatefulWidget {
  const PostListPage({super.key});

  @override
  ConsumerState<PostListPage> createState() => _PostListPageState();
}

class _PostListPageState extends ConsumerState<PostListPage> with SingleTickerProviderStateMixin {
  late final TabController _tab = TabController(length: 2, vsync: this)..addListener(() => setState(() {}));
  String? _tag;

  @override
  void dispose() {
    _tab.dispose();
    super.dispose();
  }

  FeedQuery get _query => FeedQuery(feed: _tab.index == 1 ? 'following' : 'all', tag: _tag);

  Future<void> _write() async {
    if (!requireLogin(context, ref)) return;
    final bool? created = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const PostWritePage()));
    if (created == true) {
      ref.invalidate(feedProvider);
      ref.invalidate(hashtagsProvider);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool loggedIn = ref.watch(authProvider.select((a) => a.isLoggedIn));
    return AppLayout(
      title: '커뮤니티',
      bottom: TabBar(controller: _tab, tabs: const [Tab(text: '전체'), Tab(text: '팔로잉')]),
      floatingActionButton: FloatingActionButton.extended(onPressed: _write, icon: const Icon(Icons.edit), label: const Text('글쓰기')),
      child: Column(children: [
        _tagBar(),
        Expanded(
          child: _tab.index == 1 && !loggedIn
              ? const EmptyView(message: '로그인하면 팔로우한 사람들의 글을 모아 볼 수 있어요.', icon: Icons.group_outlined)
              : _feed(),
        ),
      ]),
    );
  }

  Widget _tagBar() {
    final AsyncValue<List<String>> tags = ref.watch(hashtagsProvider);
    final List<String> list = tags.maybeWhen(data: (t) => t, orElse: () => const <String>[]);
    if (list.isEmpty && _tag == null) return const SizedBox.shrink();
    return SizedBox(
      height: 48,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        children: [
          Padding(
            padding: const EdgeInsets.all(4),
            child: ChoiceChip(label: const Text('전체'), selected: _tag == null, onSelected: (_) => setState(() => _tag = null)),
          ),
          for (final String t in list)
            Padding(
              padding: const EdgeInsets.all(4),
              child: ChoiceChip(label: Text('#$t'), selected: _tag == t, onSelected: (_) => setState(() => _tag = _tag == t ? null : t)),
            ),
        ],
      ),
    );
  }

  Widget _feed() {
    final FeedQuery q = _query;
    final AsyncValue<List<Map<String, dynamic>>> posts = ref.watch(feedProvider(q));
    return posts.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => EmptyView(message: errorMessage(e), onRetry: () => ref.invalidate(feedProvider(q))),
      data: (list) => list.isEmpty
          ? EmptyView(
              message: q.feed == 'following'
                  ? '팔로우한 사람의 글이 없어요.\n글쓴이 닉네임을 눌러 팔로우해 보세요.'
                  : _tag != null ? '#$_tag 태그가 붙은 글이 없습니다.' : '첫 글을 남겨 보세요!',
              icon: Icons.forum_outlined,
            )
          : RefreshIndicator(
              onRefresh: () async {
                ref.invalidate(feedProvider(q));
                ref.invalidate(hashtagsProvider);
              },
              child: ListView.builder(
                padding: const EdgeInsets.only(bottom: 80),
                itemCount: list.length,
                itemBuilder: (context, i) => PostCard(
                  key: ValueKey('${list[i]['id']}-${list[i]['retweetedBy']}'),
                  post: list[i],
                  onTagTap: (t) => setState(() => _tag = t),
                  onChanged: () => ref.invalidate(feedProvider(q)),
                ),
              ),
            ),
    );
  }
}
