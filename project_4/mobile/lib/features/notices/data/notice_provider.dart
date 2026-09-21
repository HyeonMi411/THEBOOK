// 전역상태관리: 공지사항 목록/상세
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:dio/dio.dart';
import '../../../core/network/api_client.dart';

class NoticeState {
  final List<dynamic> notices;
  final bool loading;
  final String? error;

  const NoticeState({this.notices = const [], this.loading = false, this.error});
}

class NoticeNotifier extends Notifier<NoticeState> {
  @override
  NoticeState build() {
    _dio = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl()));
    return const NoticeState();
  }

  late final Dio _dio;

  // 공지사항 전체조회 (GET /api/notices, 로그인 불필요)
  Future<void> fetchNotices() async {
    state = NoticeState(notices: state.notices, loading: true, error: null);
    try {
      final response = await _dio.get('/api/notices');
      final data = response.data;
      final list = (data is Map && data.containsKey('results')) ? data['results'] : data;
      state = NoticeState(notices: list ?? [], loading: false, error: null);
    } catch (err) {
      state = NoticeState(notices: state.notices, loading: false, error: '공지사항 조회 실패: ${err.toString()}');
    }
  }

  // 공지사항 단건조회 (GET /api/notices/{id}, 조회수 증가)
  Future<Map<String, dynamic>?> fetchNoticeDetail(int id) async {
    try {
      final response = await _dio.get('/api/notices/$id');
      return response.data as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }
}

final noticeProvider = NotifierProvider<NoticeNotifier, NoticeState>(() {
  return NoticeNotifier();
});
