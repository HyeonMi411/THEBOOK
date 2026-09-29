import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/data/auth_provider.dart';
import '../features/chatbot/presentation/chatbot_page.dart';

/// 탭 화면 공통 레이아웃 - 제목 + 상담 챗봇(🎧) + 로그인/로그아웃
/// (예전에는 모든 탭의 제목이 '마이페이지'로 고정되어 있었음)
class AppLayout extends ConsumerWidget {
  final String title;
  final Widget child;
  final List<Widget> actions;
  final Widget? floatingActionButton;
  final PreferredSizeWidget? bottom;

  const AppLayout({
    super.key,
    required this.title,
    required this.child,
    this.actions = const [],
    this.floatingActionButton,
    this.bottom,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AuthState auth = ref.watch(authProvider);
    return Scaffold(
      appBar: AppBar(
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        bottom: bottom,
        actions: [
          ...actions,
          IconButton(
            icon: const Icon(Icons.support_agent),
            tooltip: '상담 챗봇',
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ChatbotPage())),
          ),
          if (!auth.isLoggedIn)
            TextButton(onPressed: () => Navigator.pushNamed(context, '/login'), child: const Text('로그인')),
        ],
      ),
      floatingActionButton: floatingActionButton,
      body: child,
    );
  }
}

/// 로그인이 필요한 동작 앞에서 호출 - 비로그인이면 안내 후 로그인 화면으로 이동하고 false
bool requireLogin(BuildContext context, WidgetRef ref) {
  if (ref.read(authProvider).isLoggedIn) return true;
  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('로그인이 필요한 서비스입니다.')));
  Navigator.pushNamed(context, '/login');
  return false;
}

/// 목록이 비었을 때 / 오류일 때 공통 안내
class EmptyView extends StatelessWidget {
  final IconData icon;
  final String message;
  final VoidCallback? onRetry;

  const EmptyView({super.key, required this.message, this.icon = Icons.inbox_outlined, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 48, color: Colors.grey),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Colors.grey)),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onRetry, child: const Text('다시 시도')),
            ],
          ],
        ),
      ),
    );
  }
}

/// 네트워크 이미지 (실패 시 아이콘)
class NetImage extends StatelessWidget {
  final String url;
  final double? width;
  final double? height;
  final BoxFit fit;
  final IconData fallbackIcon;

  const NetImage(this.url,
      {super.key, this.width, this.height, this.fit = BoxFit.cover, this.fallbackIcon = Icons.menu_book});

  @override
  Widget build(BuildContext context) {
    final Widget fallback = Container(
      width: width,
      height: height,
      color: Colors.grey[200],
      alignment: Alignment.center,
      child: Icon(fallbackIcon, color: Colors.grey),
    );
    if (url.isEmpty) return fallback;
    return Image.network(url, width: width, height: height, fit: fit, errorBuilder: (_, _, _) => fallback);
  }
}
