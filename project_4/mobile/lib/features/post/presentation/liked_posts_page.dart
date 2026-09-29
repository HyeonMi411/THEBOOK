import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/app_layout.dart';
import '../data/board_provider.dart';
import 'post_card.dart';

class LikedPostsPage extends ConsumerWidget {
  const LikedPostsPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> posts = ref.watch(likedPostsProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('좋아요한 글')),
      body: posts.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e), onRetry: () => ref.invalidate(likedPostsProvider)),
        data: (list) => list.isEmpty
            ? const EmptyView(message: '좋아요한 글이 없어요.', icon: Icons.favorite_border)
            : ListView(children: [
                for (final Map<String, dynamic> p in list)
                  PostCard(key: ValueKey(p['id']), post: p, onChanged: () => ref.invalidate(likedPostsProvider)),
              ]),
      ),
    );
  }
}
