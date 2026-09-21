// 전역상태관리 : REACT의 REDUX
import 'package:flutter_riverpod/flutter_riverpod.dart';
// 비동기처리   : REACT의 AXIOS
import 'package:dio/dio.dart';
// 보안저장소
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
// 모바일기기에서 갤러리/ 카메라 접근라이브러리
import 'package:image_picker/image_picker.dart';
// 서버 BASE URL : 10.0.0.2 / localhost:8080 / https://thejoa703.ducksdns.org
import '../../../core/network/api_client.dart';

/////// part1) 게시판의 상태 
class BoardState {
  final List<dynamic> posts;
  final bool loading;
  final String? error;

  const BoardState({
    this.posts = const [],
    this.loading = false,
    this.error,
  });
}
/////// part2) saga, 최신상태 반영
class BoardNotifier extends Notifier<BoardState> {
  @override
  BoardState build() {  // Notifier 초기화 + reducer 초기상태
    _initDio();
    return const BoardState();
  }

  late final Dio _dio;  // late 지연초기화 Dio 객체초기화
  final _storage = const FlutterSecureStorage();  // 보안저장소

  // 설정
  void _initDio() {
    _dio = Dio(BaseOptions(  // 기본옵션 설정
      baseUrl: ApiClient.getBaseUrl(),  // 10.0.0.2 / localhost:8080 / https://thejoa703.ducksdns.org
    ));

    // 인증 토큰 자동 첨부 + 만료 시 자동 재발급 인터셉터
    _dio.interceptors.add( InterceptorsWrapper (    // InterceptorsWrapper
    // 요청 
      onRequest: (options, handler) async {
        final token = await _storage.read(key: 'accessToken');  // 토큰읽기
        if (token != null) {  // 토큰있으면
          options.headers['Authorization'] = 'Bearer $token'; // http 헤더에 Bearer
        }
        return handler.next(options);
      },
      // 에러 
      onError: (DioException e, handler) async {
        // 401(인증 실패, 토큰 만료)일 때만 재발급 시도
        if (e.response?.statusCode == 401) {
          print('⚠️ [board_provider] 401 감지 - 토큰 재발급 시도');
          final refreshed = await _refreshAccessToken();  // 재발급시도 
          if (refreshed) {
            final newToken = await _storage.read(key: 'accessToken');
            e.requestOptions.headers['Authorization'] = 'Bearer $newToken';
            try {
              // 새 토큰으로 원래 요청 재시도
              final cloned = await _dio.fetch(e.requestOptions);
              return handler.resolve(cloned);
            } catch (retryErr) {
              print('❌ [board_provider] 재시도 요청도 실패: $retryErr');
              return handler.next(e);
            }
          } else {
            print('❌ [board_provider] 토큰 재발급 실패');
          }
        }
        return handler.next(e);
      },
    ));
  }

  // 리프레시 토큰으로 새 accessToken 재발급
  Future<bool> _refreshAccessToken() async {
    try {
      // 인터셉터 무한루프 방지를 위해 별도 Dio 인스턴스 사용
      final refreshDio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
      final response = await refreshDio.post('/auth/refresh');
      final newAccessToken = response.data['accessToken'];  // map 토큰추출
      if (newAccessToken != null) {
        await _storage.write(key: 'accessToken', value: newAccessToken);
        return true;
      }
      return false;
    } catch (err) {
      print('❌ [board_provider] refresh 요청 실패: $err');
      return false;
    }
  }

  // 1. 전체 게시글 조회 (GET /api/posts)
  Future<void> fetchPosts() async {
    state = BoardState(posts: state.posts, loading: true, error: null);
    try {
      final response = await _dio.get('/api/posts');  // boot 게시글목록 요청
      final List<dynamic> fetchedPosts = response.data is List ? response.data : [];
      state = BoardState(posts: fetchedPosts, loading: false, error: null); // 데이터 업데이트 
    } catch (err) {
      state = BoardState(posts: state.posts, loading: false, error: '조회 실패: ${err.toString()}');
    }
  }

  // 2. 게시글 작성 (POST /api/posts - 멀티파트 + 파일 바이트 전송으로 용량 안정화 + userId 누락 방지)
  Future<bool> createPost({
    required String userId,
    required String content,
    String? hashtags,
    List<XFile>? imageFiles,
  }) async {
    try {
      //  작성자(userId)가 누락되어 익명으로 뜨는 문제를 방지하기 위해 빈 값이 아닐 때만 안전하게 포함
      final Map<String, dynamic> dataMap = {  // 전송할 Map(Json)
        'content': content,
        if (hashtags != null && hashtags.isNotEmpty) 'hashtags': hashtags,
      };
      
      if (userId.isNotEmpty) {
        dataMap['userId'] = userId;
      }

      FormData formData = FormData.fromMap(dataMap);  // multipart/form-data 객체 생성

      //  패키지 오류 없이 XFile의 바이트를 직접 읽어 전송하여 업로드 안정성 확보
      if (imageFiles != null && imageFiles.isNotEmpty) {  // 첨부이미지가 있는경우
        for (var image in imageFiles) {
          final bytes = await image.readAsBytes();  // 파일으 바이너리 바이트 배열 읽기
          formData.files.add(MapEntry(
            'files',  // boot - RequestPar("files")
            MultipartFile.fromBytes(  // MultipartFile
              bytes,
              filename: image.name.isNotEmpty ? image.name : 'upload.jpg',
            ),
          ));
        }
      }

      await _dio.post('/api/posts', data: formData);    //   MultipartFile - post요청 전송
      await fetchPosts();// 글다쓰고 나서 게시글 목록새로고침
      return true;
    } on DioException catch (e) {
      print('❌ [createPost DioException]: ${e.response?.statusCode} - ${e.response?.data}');
      return false;
    } catch (err) {
      print('❌ [createPost Unknown Error]: $err');
      return false;
    }
  }


}

final boardProvider = NotifierProvider<BoardNotifier, BoardState>(() {  // 1.  NotifierProvider  전역프로바이더 정의
  return BoardNotifier();
});
