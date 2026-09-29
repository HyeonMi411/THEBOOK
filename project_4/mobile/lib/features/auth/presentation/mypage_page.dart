import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../books/presentation/external_search_page.dart';
import '../../map/presentation/store_map_page.dart';
import '../../orders/presentation/orders_page.dart';
import '../../post/presentation/liked_posts_page.dart';
import '../../post/presentation/user_profile_page.dart';
import '../data/auth_provider.dart';

class MyPage extends ConsumerWidget {
  const MyPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AuthState auth = ref.watch(authProvider);
    if (!auth.restored) {
      return const AppLayout(title: '마이', child: Center(child: CircularProgressIndicator()));
    }
    if (!auth.isLoggedIn) {
      return AppLayout(
        title: '마이',
        child: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.person_outline, size: 56, color: Colors.grey),
            const SizedBox(height: 12),
            const Text('로그인하고 주문내역과 커뮤니티 활동을 확인하세요.'),
            const SizedBox(height: 16),
            FilledButton(onPressed: () => Navigator.pushNamed(context, '/login'), child: const Text('로그인')),
            TextButton(onPressed: () => Navigator.pushNamed(context, '/signup'), child: const Text('회원가입')),
          ]),
        ),
      );
    }
    final Map<String, dynamic> user = auth.user!;
    return AppLayout(
      title: '마이',
      child: ListView(
        children: [
          const SizedBox(height: 20),
          Center(
            child: Stack(children: [
              CircleAvatar(
                radius: 44,
                backgroundColor: Colors.blue.shade50,
                backgroundImage: (user['ufile'] ?? '').toString().isNotEmpty ? NetworkImage(imageUrl(user['ufile'])) : null,
                child: (user['ufile'] ?? '').toString().isEmpty ? const Icon(Icons.person, size: 44) : null,
              ),
              Positioned(
                right: 0,
                bottom: 0,
                child: InkWell(
                  onTap: () => _changeImage(context, ref),
                  child: const CircleAvatar(radius: 15, child: Icon(Icons.camera_alt, size: 16)),
                ),
              ),
            ]),
          ),
          const SizedBox(height: 10),
          Center(
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Text(auth.nickname, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              IconButton(icon: const Icon(Icons.edit, size: 18), onPressed: () => _changeNickname(context, ref)),
            ]),
          ),
          Center(child: Text('${user['email'] ?? ''}${auth.isAdmin ? ' · 관리자' : ''}', style: const TextStyle(color: Colors.grey))),
          const SizedBox(height: 16),
          const Divider(),
          _tile(context, Icons.receipt_long, '주문내역', const OrdersPage()),
          _tile(context, Icons.article_outlined, '내 커뮤니티 프로필 (글·리트윗·팔로워)', UserProfilePage(userId: auth.userId!)),
          _tile(context, Icons.favorite_border, '좋아요한 글', const LikedPostsPage()),
          _tile(context, Icons.map_outlined, '매장 지도', const StoreMapPage()),
          if (auth.isAdmin) _tile(context, Icons.library_add_outlined, '외부 도서 검색·등록 (관리자)', const ExternalSearchPage()),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.logout),
            title: const Text('로그아웃'),
            onTap: () async {
              await ref.read(authProvider.notifier).logout();
              if (context.mounted) {
                ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('로그아웃되었습니다.')));
              }
            },
          ),
          ListTile(
            leading: const Icon(Icons.person_remove_outlined, color: Colors.red),
            title: const Text('회원 탈퇴', style: TextStyle(color: Colors.red)),
            onTap: () => _withdraw(context, ref),
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _tile(BuildContext context, IconData icon, String label, Widget page) {
    return ListTile(
      leading: Icon(icon),
      title: Text(label),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => page)),
    );
  }

  Future<void> _changeImage(BuildContext context, WidgetRef ref) async {
    final XFile? file = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 800, imageQuality: 85);
    if (file == null) return;
    final String? err = await ref.read(authProvider.notifier).updateProfileImage(file);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err ?? '프로필 사진을 바꿨습니다.')));
    }
  }

  Future<void> _changeNickname(BuildContext context, WidgetRef ref) async {
    final TextEditingController c = TextEditingController(text: ref.read(authProvider).nickname);
    final String? value = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('닉네임 변경'),
        content: TextField(controller: c, maxLength: 20, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('취소')),
          FilledButton(onPressed: () => Navigator.pop(ctx, c.text.trim()), child: const Text('변경')),
        ],
      ),
    );
    c.dispose();
    if (value == null || value.isEmpty) return;
    final String? err = await ref.read(authProvider.notifier).updateNickname(value);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err ?? '닉네임을 바꿨습니다.')));
    }
  }

  Future<void> _withdraw(BuildContext context, WidgetRef ref) async {
    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('회원 탈퇴'),
        content: const Text('탈퇴하면 같은 계정으로 다시 로그인할 수 없습니다.\n주문·결제 이력은 기록 보존을 위해 남습니다.\n정말 탈퇴하시겠어요?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('취소')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('탈퇴'),
          ),
        ],
      ),
    );
    if (ok != true) return;
    final String? err = await ref.read(authProvider.notifier).withdraw();
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err ?? '탈퇴가 완료되었습니다.')));
    }
  }
}
