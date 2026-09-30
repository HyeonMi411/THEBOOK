import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/network/upload.dart';

/// 도서 목록 상태 (페이지 단위로 이어 붙이는 무한 스크롤)
class BookListState {
  final List<Map<String, dynamic>> books;
  final int page;          // 마지막으로 불러온 페이지 (1부터)
  final int totalPages;
  final bool loading;
  final String keyword;    // 제목 검색어 (비어 있으면 전체 목록)
  final String? error;

  const BookListState({
    this.books = const [],
    this.page = 0,
    this.totalPages = 1,
    this.loading = false,
    this.keyword = '',
    this.error,
  });

  bool get hasMore => keyword.isEmpty && page < totalPages;
}

class BookListNotifier extends Notifier<BookListState> {
  Dio get _dio => DioClient.instance;

  @override
  BookListState build() {
    Future.microtask(refresh);
    return const BookListState(loading: true);
  }

  Future<void> refresh() async {
    state = BookListState(loading: true, keyword: state.keyword);
    if (state.keyword.isNotEmpty) {
      await search(state.keyword);
    } else {
      await loadMore();
    }
  }

  /// GET /api/books?page=&size=12  → PageResponseDto { content, currentPage, totalPages }
  Future<void> loadMore() async {
    if (state.page > 0 && (!state.hasMore || state.loading && state.books.isNotEmpty)) return;
    final int next = state.page + 1;
    state = BookListState(books: state.books, page: state.page, totalPages: state.totalPages, loading: true);
    try {
      final Response<dynamic> res = await _dio.get('/api/books', queryParameters: {'page': next, 'size': 12});
      final Map<dynamic, dynamic> data = res.data as Map;
      final List<Map<String, dynamic>> items =
          (data['content'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
      state = BookListState(
        books: [...state.books, ...items],
        page: next,
        totalPages: (data['totalPages'] as num?)?.toInt() ?? next,
      );
    } catch (e) {
      state = BookListState(books: state.books, page: state.page, totalPages: state.totalPages, error: errorMessage(e));
    }
  }

  /// GET /api/books/search?keyword=  (제목 검색, 페이징 없음)
  Future<void> search(String keyword) async {
    final String k = keyword.trim();
    if (k.isEmpty) {
      state = const BookListState(loading: true);
      return loadMore();
    }
    state = BookListState(loading: true, keyword: k);
    try {
      final Response<dynamic> res = await _dio.get('/api/books/search', queryParameters: {'keyword': k});
      final List<Map<String, dynamic>> items =
          (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
      state = BookListState(books: items, page: 1, totalPages: 1, keyword: k);
    } catch (e) {
      state = BookListState(keyword: k, error: errorMessage(e));
    }
  }
}

final bookListProvider = NotifierProvider<BookListNotifier, BookListState>(BookListNotifier.new);

/// 도서 상세 GET /api/books/{id}
final bookDetailProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, int>((ref, id) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/books/$id');
  return Map<String, dynamic>.from(res.data as Map);
});

/// 베스트셀러 TOP 10 (결제완료 판매량 기준, 서버 Redis 캐시)
final bestsellerProvider = FutureProvider.autoDispose<List<Map<String, dynamic>>>((ref) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/books/bestsellers');
  return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
});

/// 외부 도서 통합검색 / 등록 / 표지 OCR
class BookApi {
  static Dio get _dio => DioClient.instance;

  /// source: kakao | naver | nl
  static Future<List<Map<String, dynamic>>> searchExternal(String source, String keyword) async {
    final Response<dynamic> res =
        await _dio.get('/api/books/external/search', queryParameters: {'source': source, 'keyword': keyword});
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  /// 관리자 전용 - 외부 검색결과 1권을 쇼핑몰에 등록
  static Future<Map<String, dynamic>> importExternal(Map<String, dynamic> book) async {
    final Response<dynamic> res = await _dio.post('/api/books/external/import', data: book);
    return Map<String, dynamic>.from(res.data as Map);
  }

  /// 실시간 검색(자동완성) - 3차 BookSearchBox 와 같은 /api/books/search, 상위 8개만
  static Future<List<Map<String, dynamic>>> suggest(String keyword) async {
    final Response<dynamic> res = await _dio.get('/api/books/search', queryParameters: {'keyword': keyword});
    return (res.data as List).take(8).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  /// 도서 등록(id == null) / 수정 (관리자, multipart - 표지는 cover 파트)
  static Future<Map<String, dynamic>> saveBook({int? id, required Map<String, dynamic> fields, XFile? cover}) async {
    final Map<String, dynamic> body = {
      for (final MapEntry<String, dynamic> e in fields.entries)
        if (e.value != null && e.value.toString().trim().isNotEmpty) e.key: e.value.toString().trim(),
    };
    if (cover != null) {
      body['cover'] = await imagePart(cover);
    }
    final FormData form = FormData.fromMap(body);
    final Response<dynamic> res = id == null
        ? await _dio.post('/api/books', data: form)
        : await _dio.patch('/api/books/$id', data: form);
    return Map<String, dynamic>.from(res.data as Map);
  }

  /// 재고 수량 변경 (관리자)
  static Future<void> updateStock(int id, int quantity) =>
      _dio.patch('/api/books/$id/stock', data: {'stockQuantity': quantity});

  /// 도서 삭제 (관리자, 서버는 소프트 삭제)
  static Future<void> deleteBook(int id) => _dio.delete('/api/books/$id');

  /// 국립중앙도서관 검색 - 키워드 또는 KDC 분류명 (3차 national-library 화면과 같은 API)
  static Future<List<Map<String, dynamic>>> nlSearch(String keyword, {int page = 1}) async {
    final Response<dynamic> res =
        await _dio.get('/api/books/national-library/search', queryParameters: {'keyword': keyword, 'page': page});
    return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }

  /// 국립중앙도서관 검색결과 1권을 BookStore 에 저장 (관리자)
  static Future<Map<String, dynamic>> nlSave(Map<String, dynamic> nlBook) async {
    final Response<dynamic> res = await _dio.post('/api/books/national-library/save', data: nlBook);
    return Map<String, dynamic>.from(res.data as Map);
  }

  /// 표지 사진 → { lines: [...], keyword: "추천 검색어" }
  static Future<Map<String, dynamic>> ocr(XFile image) async {
    final FormData form = FormData.fromMap({'image': await imagePart(image)});
    final Response<dynamic> res = await _dio.post('/api/util/ocr', data: form);
    return Map<String, dynamic>.from(res.data as Map);
  }
}
