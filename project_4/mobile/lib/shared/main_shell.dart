// 하단 탭 - project_3(Next.js)의 상단 메뉴(BOOK/NOTICE/CART/게시판/MYPAGE)를 모바일 하단 탭으로 재구성
import 'package:flutter/material.dart';

import '../features/auth/presentation/mypage_page.dart';
import '../features/books/presentation/book_list_page.dart';
import '../features/cart/presentation/cart_page.dart';
import '../features/home/presentation/home_page.dart';
import '../features/post/presentation/post_list_page.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  /// 다른 화면에서 탭을 바꿀 때 사용 (예: 홈의 "도서 더보기" → 도서 탭)
  static final ValueNotifier<int> tabIndex = ValueNotifier<int>(0);

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  static const List<Widget> _pages = [
    HomePage(),
    BookListPage(),
    PostListPage(),
    CartPage(),
    MyPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<int>(
      valueListenable: MainShell.tabIndex,
      builder: (context, index, _) => Scaffold(
        body: IndexedStack(index: index, children: _pages),
        bottomNavigationBar: NavigationBar(
          selectedIndex: index,
          onDestinationSelected: (i) => MainShell.tabIndex.value = i,
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: '홈'),
            NavigationDestination(icon: Icon(Icons.menu_book_outlined), selectedIcon: Icon(Icons.menu_book), label: '도서'),
            NavigationDestination(icon: Icon(Icons.forum_outlined), selectedIcon: Icon(Icons.forum), label: '커뮤니티'),
            NavigationDestination(icon: Icon(Icons.shopping_cart_outlined), selectedIcon: Icon(Icons.shopping_cart), label: '장바구니'),
            NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: '마이'),
          ],
        ),
      ),
    );
  }
}
