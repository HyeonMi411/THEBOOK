import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 리프레시 토큰 쿠키 처리 (모바일 전용 보완)
///
/// 서버(Spring Boot)는 리프레시 토큰을 응답 본문이 아니라 HttpOnly 쿠키
/// (Set-Cookie: refreshToken=...) 로만 내려주고, POST /auth/refresh 는 그 쿠키를 읽어 새 액세스 토큰을 발급한다.
/// 웹 브라우저는 쿠키를 자동으로 저장·전송하지만, 모바일의 Dio 는 쿠키를 저장하지 않기 때문에
/// 그대로 두면 재발급이 항상 실패해서 액세스 토큰이 만료되는 15분 뒤에 강제 로그아웃이 된다.
///
/// 그래서 로그인 응답의 Set-Cookie 에서 refreshToken 값을 꺼내 보안 저장소에 보관해 두었다가,
/// 재발급 요청 때 Cookie 헤더로 직접 실어 보낸다.
class RefreshCookie {
  static const FlutterSecureStorage _storage = FlutterSecureStorage();
  static const String _key = 'refreshToken';
  static const String _prefix = 'refreshToken=';

  /// 응답 헤더(Set-Cookie)에 refreshToken 이 있으면 저장한다. 값이 비어 있으면(=삭제 쿠키) 저장된 값을 지운다.
  static Future<void> saveFromResponse(Response<dynamic> response) async {
    final List<String>? cookies = response.headers['set-cookie'];
    if (cookies == null) {
      return;
    }
    for (final String cookie in cookies) {
      if (!cookie.startsWith(_prefix)) {
        continue;
      }
      final int end = cookie.indexOf(';');
      final String value = cookie.substring(_prefix.length, end == -1 ? cookie.length : end);
      if (value.isEmpty) {
        await _storage.delete(key: _key);
      } else {
        await _storage.write(key: _key, value: value);
      }
    }
  }

  /// 재발급 요청(POST /auth/refresh)에 사용할 옵션. 저장된 리프레시 토큰이 없으면 null.
  static Future<Options?> cookieOptions() async {
    final String? token = await _storage.read(key: _key);
    if (token == null || token.isEmpty) {
      return null;
    }
    return Options(headers: <String, dynamic>{'Cookie': '$_prefix$token'});
  }

  /// 로그아웃 시 저장된 리프레시 토큰 삭제
  static Future<void> clear() async {
    await _storage.delete(key: _key);
  }
}
