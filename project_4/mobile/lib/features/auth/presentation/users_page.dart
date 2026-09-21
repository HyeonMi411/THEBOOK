import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../shared/components/app_layout.dart';
import '../data/auth_provider.dart';
import '../../orders/presentation/orders_page.dart';

class UsersPage extends ConsumerWidget {
  const UsersPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // [핵심] 읽어온 유저 데이터 가져오기
    final user = ref.watch(authProvider).user;

    return AppLayout(
      child: Scaffold(
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: user == null
              ? const Center(child: Text('로그인이 필요합니다.'))
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // 카드형태의 표
                    Card(
                      elevation: 2,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        // HTML의 <table> 태그 역할을 해주는 Flutter 내장 표 위젯
                        child: DataTable(
                          columnSpacing: 24.0,
                          columns: const [
                            DataColumn(
                              label: Text(
                                'NO',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                            ),
                            DataColumn(
                              label: Text(
                                'MYINFO',
                                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                            ),
                          ],
                          rows: [
                            DataRow(cells: [
                              const DataCell(Text('닉네임', style: TextStyle(fontWeight: FontWeight.w600))),
                              DataCell(Text('${user['nickname'] ?? '-'}')),
                            ]),
                            DataRow(cells: [
                              const DataCell(Text('이메일', style: TextStyle(fontWeight: FontWeight.w600))),
                              DataCell(Text('${user['email'] ?? '-'}')),
                            ]),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                    // project_4 신규: 주문내역 보기
                    ElevatedButton.icon(
                      onPressed: () {
                        Navigator.push(context, MaterialPageRoute(builder: (_) => const OrdersPage()));
                      },
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      icon: const Icon(Icons.receipt_long),
                      label: const Text('주문내역 보기', style: TextStyle(fontSize: 16)),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton.icon(
                      onPressed: () {
                        Navigator.pushNamedAndRemoveUntil(context, '/', (route) => false);
                      },
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                      icon: const Icon(Icons.list),
                      label: const Text('게시글 목록으로 이동', style: TextStyle(fontSize: 16)),
                    ),
                  ],
                ),
        ),
        
        // 로그인 된 상태, 화면 우측 하단 둥근 플로팅 글쓰기 버튼 (FAB)
        floatingActionButton: user != null
            ? FloatingActionButton.extended(
                onPressed: () {
                  Navigator.pushNamed(context, '/post-write');
                },
                icon: const Icon(Icons.edit),
                label: const Text('글쓰기'),
              )
            : null,
      ),
    );
  }
}
