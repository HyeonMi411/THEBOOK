import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/auth/data/auth_provider.dart';

class AppLayout extends ConsumerWidget {
  final Widget child;
  const AppLayout({super.key, required this.child});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider); // 전역 인증상태 관찰
    final bool isLogined = authState.accessToken != null && authState.user != null;
    final userNickname = authState.user?['nickname'] ?? '유저';

    return Scaffold(
      appBar: AppBar(
        title: const Text('마이페이지'),
        actions: [  // {isLogined?  <A/> : <B/>}
          if (isLogined) ...[
            Center(
              // [핵심] 일반 Text/Container 영역을 클릭 및 물결 터치(Ripple) 반응형으로 만들어 주는 위젯
              child: InkWell(
                onTap: () {
                  Navigator.pushNamed(context, '/users');
                },
                borderRadius: BorderRadius.circular(4.0),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
                  child: Text(
                    '$userNickname님 환영합니다!',
                    style: const TextStyle(
                      decoration: TextDecoration.underline,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 4),
            TextButton(
              onPressed: () {
                // [핵심] 읽기 전용으로 Notifier의 메서드 실행
                ref.read(authProvider.notifier).logout();
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('로그아웃 되었습니다.')),
                );
              },
              child: const Text('로그아웃', style: TextStyle(color: Colors.white)),
            ),
          ] else ...[
            TextButton(
              onPressed: () => Navigator.pushNamed(context, '/login'),
              child: const Text('로그인', style: TextStyle(color: Colors.white)),
            ),
          ],
        ],
      ),
      body: child,
    );
  }
}

// //  React에서 레이아웃 : <Layout>{children}</Layout>
// //  ConsumerWidget 상속받으면  React에서 useSelector 처럼 전역인증

// import 'package:flutter/material.dart';
// import 'package:flutter_riverpod/flutter_riverpod.dart';

// // Riverpod 상태를 반응형으로 관찰하기 위해 StatelessWidget 대신 상속받는 클래스
// class AppLayout extends ConsumerWidget {
//   final Widget child;   // React의 Children
//   const AppLayout({super.key, required this.child});

//   @override
//   Widget build(BuildContext context, WidgetRef ref) {
//     // 임시테스트용 - 로그인 여부테스트
//     final bool isLogined = DateTime.now().year > 2000;

//     return Scaffold(
//       appBar: AppBar(
//         title: const Text('나의 소셜 앱'),
//         actions: [
//           //  ... 전개연산자  React     { isLogined ? <A/> : <B/>  }
//           if (isLogined) ...[
//             const Center(
//               child: Padding(
//                 padding: EdgeInsets.symmetric(horizontal: 8.0),
//                 child: Text('테스트유저님 환영합니다!'),
//               ),
//             ),
//           ] else ...[
//             TextButton(
//               // 지정한 라우트 경로 이름('/login')으로 화면 전환
//               onPressed: () => Navigator.pushNamed(context, '/login'),
//               child: const Text('로그인', style: TextStyle(color: Colors.white)),
//             ),
//           ],
//         ],
//       ),
//       body: child,
//     );
//   }
// }
