import 'package:flutter/material.dart';
import 'shared/components/main_shell.dart';
import 'features/auth/presentation/login_page.dart';
import 'features/auth/presentation/signup_page.dart';
import 'features/auth/presentation/users_page.dart';
import 'features/post/presentation/post_write_page.dart';

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BookStore',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.blue,
          foregroundColor: Colors.white,
        ),
      ),
      initialRoute: '/',
      // 도서/게시판/공지/장바구니/마이페이지는 하단 탭(MainShell) 안에서 전환되고,
      // 로그인·회원가입·글쓰기처럼 별도 화면으로 완전히 이동해야 하는 것만 named route로 유지.
      routes: {
        '/': (context) => const MainShell(),
        '/login': (context) => const LoginPage(),
        '/signup': (context) => const SignupPage(),
        '/users': (context) => const UsersPage(),
        '/post-write': (context) => const PostWritePage(),
      },
    );
  }
}
