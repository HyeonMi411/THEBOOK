import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../shared/components/app_layout.dart';
import '../data/board_provider.dart';
import '../../auth/data/auth_provider.dart';
import 'post_detail_page.dart';
import '../../../core/network/api_client.dart';

class PostListPage extends ConsumerStatefulWidget {
  const PostListPage({super.key});

  @override
  ConsumerState<PostListPage> createState() => _PostListPageState();
}

class _PostListPageState extends ConsumerState<PostListPage> {
  // 이미지 URL을 안전하게 완성하는 헬퍼 메서드
  String _resolveImageUrl(String url) { //url변환
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    final String serverBaseUrl = ApiClient.getBaseUrl();  // https://d2big.ducksdns.org , localhost:8080
    final cleanBase = serverBaseUrl.endsWith('/')   // 서버 도메인 추출
        ? serverBaseUrl.substring(0, serverBaseUrl.length - 1)
        : serverBaseUrl;
    final cleanUrl = url.startsWith('/') ? url : '/$url';
    return '$cleanBase$cleanUrl';
  }

  @override
  void initState() {  // 위젯연결시 1번 
    super.initState();
    // 화면그림그리기
    Future.microtask(() => ref.read(boardProvider.notifier).fetchPosts());
  }// react useEffect( ..... , []) 와 동일 - 1번 읽어들임

  @override
  Widget build(BuildContext context) {
    final boardState = ref.watch(boardProvider); // watch  지속적으로 확인       useEffect( ..... , [user])
    final authState = ref.watch(authProvider); // 로그인 상태 감지

    return AppLayout(// 공통레이아웃
      child: Scaffold(
        body: boardState.loading && boardState.posts.isEmpty    //조건1:  로딩중?
            ? const Center(child: CircularProgressIndicator())  // 로딩화면 
            : boardState.posts.isEmpty    //조건2:  비었다면
                ? const Center(child: Text('등록된 게시글이 없습니다.'))
                : RefreshIndicator(   // 모바일당겨서 새로고침 - RefreshIndicator
                    onRefresh: () async {
                      await ref.read(boardProvider.notifier).fetchPosts();  // 게시글가져와주는 api
                    },
                    child: ListView.builder(  // 리스트
                      itemCount: boardState.posts.length,
                      itemBuilder: (context, index) {
                        final post = boardState.posts[index];

                        // 백엔드 API 응답 키(authorNickname 최우선) 안전하게 추출
                        final String nickname = post['authorNickname'] ??
                            post['userNickname'] ??
                            post['nickname'] ??
                            post['writerNickname'] ??
                            post['user']?['nickname'] ??
                            '익명';

                        final String content = post['content'] ?? '';
                        final List<dynamic> hashtags = post['hashtags'] ?? [];
                        final List<dynamic> imageUrls = post['imageUrls'] ?? [];

                        return Card(    // 카드스타일
                          margin: const EdgeInsets.symmetric(
                              horizontal: 12, vertical: 6),   // 카드 외각 마진설정
                          child: InkWell(
                            onTap: () {
                              Navigator.push(   // 상세페이지로 이동
                                context,
                                MaterialPageRoute(
                                  builder: (context) =>
                                      PostDetailPage(post: post), // 상세페이지 위젯
                                ),
                              );
                            },
                            child: Padding(
                              padding: const EdgeInsets.all(12.0),
                              child: Column(    // 세로정렬레이아웃
                                crossAxisAlignment: CrossAxisAlignment.start, // 좌측정렬
                                children: [
                                  Text(
                                    '작성자: $nickname',
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      color: Colors.blue,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    content,
                                    style: const TextStyle(fontSize: 16),
                                    maxLines: 2,  // 최대 2줄만보이게
                                    overflow: TextOverflow.ellipsis,  // 말줄임표 ...
                                  ),
                                  const SizedBox(height: 6),
                                  if (hashtags.isNotEmpty)  
                                    Wrap(  // flex-wrap
                                      spacing: 6.0,  // 요소간의 간격
                                      children: hashtags
                                          .map(
                                            (tag) => Text(
                                              tag.toString().startsWith('#')
                                                  ? tag.toString()
                                                  : '#$tag',
                                              style: const TextStyle(
                                                  color: Colors.indigo),
                                            ),
                                          )
                                          .toList(),
                                    ),
                                  if (imageUrls.isNotEmpty) ...[
                                    const SizedBox(height: 8),
                                    ClipRRect(  // 자식이미지 모서리 둥글게
                                      borderRadius: BorderRadius.circular(6.0),
                                      child: Image.network(
                                        _resolveImageUrl(
                                            imageUrls.first.toString()),  // 이미지 주소 
                                        height: 120,
                                        width: double.infinity,
                                        fit: BoxFit.cover,  // 이미지 비율 꽉채우기 object-fit : cover
                                        errorBuilder: // 이미지 실패시
                                            (context, error, stackTrace) =>
                                                Container(
                                          height: 120,
                                          color: Colors.grey[200],
                                          alignment: Alignment.center,
                                          child: const Text(
                                            '이미지를 불러올 수 없습니다.',
                                            style: TextStyle(
                                                color: Colors.grey,
                                                fontSize: 12),
                                          ),
                                        ),
                                      ),
                                    ),
                                  ]
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
        floatingActionButton: FloatingActionButton(   // 화면 우측하단 플로팅 글스기 버튼
          onPressed: () { // 버튼클릭시
            if (authState.user == null && authState.accessToken == null) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('로그인이 필요한 서비스입니다.')),
              );
              Navigator.pushNamed(context, '/login'); // pushNamed  - 로그인화면이동
            } else {
              Navigator.pushNamed(context, '/post-write');  // 작성화면
            }
          },
          child: const Icon(Icons.create),   // 연필아이콘 
        ),
      ),
    );
  }
}
