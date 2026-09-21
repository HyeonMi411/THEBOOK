// 전역상태관리: 도서 목록/상세 조회
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/network/api_client.dart';

class BookState {
  final List<dynamic> books;
  final bool loading;
  final String? error;

  const BookState({this.books = const [], this.loading = false, this.error});
}

class BookNotifier extends Notifier<BookState> {
  @override
  BookState build() {
    _initDio();
    return const BookState();
  }

  late final Dio _dio;
  final _storage = const FlutterSecureStorage();

  void _initDio() {
    _dio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
    // 비로그인 상태에서도 도서 목록/상세는 조회 가능하지만, 로그인 중이면 토큰을 함께 실어 보냄
    _dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) async {
      final token = await _storage.read(key: 'accessToken');
      if (token != null) options.headers['Authorization'] = 'Bearer $token';
      return handler.next(options);
    }));
  }

  // 1. 도서 목록 조회 (GET /api/books, 페이지네이션 응답의 results만 추출)
  Future<void> fetchBooks({String? category}) async {
    state = BookState(books: state.books, loading: true, error: null);
    try {
      final response = await _dio.get('/api/books', queryParameters: {
        if (category != null) 'category': category,
      });
      final data = response.data;
      final list = (data is Map && data.containsKey('results')) ? data['results'] : data;
      state = BookState(books: list ?? [], loading: false, error: null);
    } catch (err) {
      state = BookState(books: state.books, loading: false, error: '도서 목록 조회 실패: ${err.toString()}');
    }
  }

  // 2. 도서 단건 조회 (GET /api/books/{id})
  Future<Map<String, dynamic>?> fetchBookDetail(int id) async {
    try {
      final response = await _dio.get('/api/books/$id');
      return response.data as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  // 3. 도서 검색 (GET /api/books/search)
  Future<void> searchBooks(String keyword) async {
    state = BookState(books: state.books, loading: true, error: null);
    try {
      final response = await _dio.get('/api/books/search', queryParameters: {'keyword': keyword});
      state = BookState(books: response.data ?? [], loading: false, error: null);
    } catch (err) {
      state = BookState(books: state.books, loading: false, error: '검색 실패: ${err.toString()}');
    }
  }
}

final bookProvider = NotifierProvider<BookNotifier, BookState>(() {
  return BookNotifier();
});
