// 하단 탭 네비게이션 - project_3(Next.js)의 상단 메뉴(BOOK/NOTICE/CART/게시판/MYPAGE)를
// 모바일 환경에 맞게 하단 탭바로 재구성
import 'package:flutter/material.dart';
import '../../features/books/presentation/book_list_page.dart';
import '../../features/post/presentation/post_list_page.dart';
import '../../features/notices/presentation/notice_list_page.dart';
import '../../features/cart/presentation/cart_page.dart';
import '../../features/auth/presentation/users_page.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _index = 0;

  final _pages = const [
    BookListPage(),
    PostListPage(),
    NoticeListPage(),
    CartPage(),
    UsersPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _index,
        type: BottomNavigationBarType.fixed,
        onTap: (i) => setState(() => _index = i),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.menu_book), label: '도서'),
          BottomNavigationBarItem(icon: Icon(Icons.forum), label: '게시판'),
          BottomNavigationBarItem(icon: Icon(Icons.campaign), label: '공지'),
          BottomNavigationBarItem(icon: Icon(Icons.shopping_cart), label: '장바구니'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: '마이페이지'),
        ],
      ),
    );
  }
}
