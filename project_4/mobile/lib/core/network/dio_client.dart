import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api_client.dart';
import 'refresh_cookie.dart';

/// 앱 전체가 함께 쓰는 Dio 하나 (예전에는 화면/Provider 마다 Dio 와 재발급 로직을 따로 만들어서
/// 동시에 여러 요청이 401 을 받으면 재발급을 여러 번 하거나, 한 곳에서만 로그아웃되는 문제가 있었다)
///
/// - 요청마다 보안 저장소의 accessToken 을 Authorization 헤더에 자동으로 실음
/// - 401 이면 리프레시 토큰(쿠키)으로 한 번만 재발급(single-flight) 후 원래 요청을 재시도
/// - 재발급까지 실패하면 토큰을 지우고 [onSessionExpired] 로 앱에 알림 → 로그인 상태 초기화
class DioClient {
  DioClient._();

  static const FlutterSecureStorage _storage = FlutterSecureStorage();
  static const String _accessKey = 'accessToken';

  static Dio? _dio;
  static Future<bool>? _refreshing;

  /// 세션 만료 시 AuthNotifier 가 등록해 두는 콜백
  static void Function()? onSessionExpired;

  static Dio get instance => _dio ??= _create();

  static Dio _create() {
    final Dio dio = Dio(BaseOptions(
      baseUrl: ApiClient.getBaseUrl(),
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 20),
      // 서버에 XML 변환기(jackson-dataformat-xml, 국립중앙도서관 파싱용)가 있어서
      // Accept 헤더에 따라 XML 로 응답할 수 있음 → 앱은 항상 JSON 만 받도록 명시
      headers: {'Accept': 'application/json'},
    ));
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (RequestOptions options, RequestInterceptorHandler handler) async {
        final String? token = await _storage.read(key: _accessKey);
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (DioException e, ErrorInterceptorHandler handler) async {
        final RequestOptions req = e.requestOptions;
        final bool isAuthCall = req.path.startsWith('/auth/refresh') || req.path.startsWith('/auth/login');
        final bool hadToken = req.headers['Authorization'] != null;
        if (e.response?.statusCode == 401 && hadToken && !isAuthCall && req.extra['retried'] != true) {
          final bool ok = await refreshAccessToken();
          if (ok) {
            final String? token = await _storage.read(key: _accessKey);
            req.headers['Authorization'] = 'Bearer $token';
            req.extra['retried'] = true;
            // 사진 업로드처럼 FormData(multipart) 요청은 한 번 전송하면 다시 쓸 수 없어서
            // ("FormData has already been finalized") 그대로 재전송하면 앱 내부 오류가 난다 → 복제해서 다시 보냄
            if (req.data is FormData) {
              req.data = (req.data as FormData).clone();
            }
            try {
              final Response<dynamic> retried = await dio.fetch(req);
              return handler.resolve(retried);
            } on DioException catch (retryError) {
              return handler.next(retryError);
            } catch (retryError) {
              return handler.next(DioException(requestOptions: req, error: retryError));
            }
          }
          await clearTokens();
          onSessionExpired?.call();
        }
        handler.next(e);
      },
    ));
    return dio;
  }

  /// 동시에 여러 요청이 401 을 받아도 재발급 요청은 한 번만 보낸다.
  static Future<bool> refreshAccessToken() {
    return _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  }

  static Future<bool> _doRefresh() async {
    final Options? cookie = await RefreshCookie.cookieOptions();
    if (cookie == null) {
      return false;
    }
    try {
      final Dio plain = Dio(BaseOptions(baseUrl: ApiClient.getBaseUrl(), headers: {'Accept': 'application/json'}));
      final Response<dynamic> res = await plain.post('/auth/refresh', options: cookie);
      await RefreshCookie.saveFromResponse(res);
      final dynamic token = res.data is Map ? res.data['accessToken'] : null;
      if (token is String && token.isNotEmpty) {
        await saveAccessToken(token);
        return true;
      }
    } catch (_) {}
    return false;
  }

  static Future<String?> readAccessToken() => _storage.read(key: _accessKey);

  static Future<void> saveAccessToken(String token) => _storage.write(key: _accessKey, value: token);

  static Future<void> clearTokens() async {
    await _storage.delete(key: _accessKey);
    await RefreshCookie.clear();
  }
}

/// 서버 에러 응답을 사람이 읽을 수 있는 한 줄로
/// - {"error": "..."} (GlobalExceptionHandler)  - {"필드": "메시지"} (@Valid)  - 네트워크 오류
String errorMessage(Object error, {String fallback = '요청을 처리하지 못했습니다.'}) {
  if (error is DioException) {
    final dynamic data = error.response?.data;
    if (data is Map) {
      if (data['error'] is String) return data['error'] as String;
      if (data['message'] is String) return data['message'] as String;
      for (final dynamic v in data.values) {
        if (v is String && v.isNotEmpty) return v;
      }
    }
    final int? status = error.response?.statusCode;
    if (status == 401) return '로그인이 필요합니다.';
    if (status == 403) return '권한이 없습니다.';
    if (status == 404) return '요청한 정보를 찾을 수 없습니다.';
    if (error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.receiveTimeout) {
      return '서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.';
    }
  }
  return fallback;
}
