import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'features/auth/data/auth_provider.dart';
import 'features/auth/presentation/login_page.dart';
import 'features/auth/presentation/signup_page.dart';
import 'features/auth/presentation/social_signup_page.dart';
import 'features/cart/data/cart_provider.dart';
import 'features/orders/data/order_provider.dart';
import 'features/orders/presentation/orders_page.dart';
import 'shared/main_shell.dart';

/// 딥링크(소셜 로그인·결제 복귀)처럼 화면 밖에서 화면을 이동/안내해야 할 때 사용
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();
final GlobalKey<ScaffoldMessengerState> messengerKey = GlobalKey<ScaffoldMessengerState>();

void showAppMessage(String message) {
  messengerKey.currentState
    ?..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}

class App extends ConsumerStatefulWidget {
  const App({super.key});

  @override
  ConsumerState<App> createState() => _AppState();
}

class _AppState extends ConsumerState<App> {
  final AppLinks _appLinks = AppLinks();
  StreamSubscription<Uri>? _linkSub;
  String? _lastHandled;

  @override
  void initState() {
    super.initState();
    Future.microtask(() => ref.read(authProvider.notifier).restoreSession());
    _listenDeepLinks();
  }

  Future<void> _listenDeepLinks() async {
    try {
      final Uri? initial = await _appLinks.getInitialLink();
      if (initial != null) _handle(initial);
    } catch (_) {}
    _linkSub = _appLinks.uriLinkStream.listen(_handle, onError: (_) {});
  }

  /// bookstore4://auth/callback?code=...        소셜 로그인 성공
  /// bookstore4://auth/callback?error=...       소셜 로그인 실패
  /// bookstore4://auth/social-signup?signupToken=...  신규 소셜회원 가입확인
  /// bookstore4://payment/complete?orderId=...  카카오페이 결제 후 복귀
  Future<void> _handle(Uri uri) async {
    if (uri.scheme != 'bookstore4' || _lastHandled == uri.toString()) return;
    _lastHandled = uri.toString(); // 같은 링크가 두 번 들어와도 한 번만 처리

    if (uri.host == 'auth' && uri.path == '/callback') {
      final String? code = uri.queryParameters['code'];
      final String? error = uri.queryParameters['error'];
      if (code != null) {
        final String? err = await ref.read(authProvider.notifier).exchangeSocialCode(code);
        if (err == null) {
          navigatorKey.currentState?.popUntil((r) => r.isFirst);
          showAppMessage('${ref.read(authProvider).nickname}님, 환영합니다!');
        } else {
          showAppMessage(err);
        }
      } else if (error != null) {
        showAppMessage(_socialError(error, uri.queryParameters['existingProvider']));
      }
    } else if (uri.host == 'auth' && uri.path == '/social-signup') {
      final String? token = uri.queryParameters['signupToken'];
      if (token != null) {
        navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => SocialSignupPage(signupToken: token)));
      }
    } else if (uri.host == 'payment') {
      ref.invalidate(cartProvider);
      ref.invalidate(ordersProvider);
      final String result = uri.path.replaceFirst('/', '');
      if (result == 'complete') {
        showAppMessage('결제가 완료되었습니다. 주문내역을 확인해 주세요.');
        navigatorKey.currentState?.push(MaterialPageRoute(builder: (_) => const OrdersPage()));
      } else if (result == 'cancel') {
        showAppMessage('결제가 취소되었습니다.');
      } else {
        showAppMessage('결제가 완료되지 않았습니다. 다시 시도해 주세요.');
      }
    }
  }

  String _socialError(String error, String? existingProvider) {
    switch (error) {
      case 'email_already_exists':
        return '이미 ${existingProvider ?? '다른 방법'}(으)로 가입된 이메일입니다. 해당 방법으로 로그인해 주세요.';
      case 'account_deleted':
        return '탈퇴한 계정입니다.';
      default:
        return '소셜 로그인에 실패했습니다. 다시 시도해 주세요.';
    }
  }

  @override
  void dispose() {
    _linkSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BookStore',
      debugShowCheckedModeBanner: false,
      navigatorKey: navigatorKey,
      scaffoldMessengerKey: messengerKey,
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF1565C0),
        appBarTheme: const AppBarTheme(centerTitle: false),
      ),
      initialRoute: '/',
      routes: {
        '/': (_) => const MainShell(),
        '/login': (_) => const LoginPage(),
        '/signup': (_) => const SignupPage(),
      },
    );
  }
}
