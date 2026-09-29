import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../data/board_provider.dart';
import 'user_profile_page.dart';

/// 팔로워 / 팔로잉 목록 (type: followers | followings)
class FollowListPage extends ConsumerWidget {
  final int userId;
  final String type;
  const FollowListPage({super.key, required this.userId, required this.type});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Map<String, dynamic>>> users = ref.watch(followListProvider((userId, type)));
    return Scaffold(
      appBar: AppBar(title: Text(type == 'followers' ? '팔로워' : '팔로잉')),
      body: users.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e)),
        data: (list) => list.isEmpty
            ? const EmptyView(message: '아직 아무도 없어요.', icon: Icons.group_outlined)
            : ListView.builder(
                itemCount: list.length,
                itemBuilder: (context, i) {
                  final Map<String, dynamic> u = list[i];
                  final String img = u['profileImage']?.toString() ?? '';
                  return ListTile(
                    leading: CircleAvatar(
                      backgroundImage: img.isNotEmpty ? NetworkImage(imageUrl(img)) : null,
                      child: img.isEmpty ? const Icon(Icons.person) : null,
                    ),
                    title: Text(u['nickname']?.toString() ?? ''),
                    onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => UserProfilePage(userId: asInt(u['id'])))),
                  );
                },
              ),
      ),
    );
  }
}
