// project_4 백엔드(Spring Boot)와 통신할 서버 base_url.
// 로그인/회원가입뿐 아니라 게시판(/api/posts)도 project_4 백엔드에서만 제공하므로,
// 반드시 project_4 서버 하나만 가리켜야 함 (project_3 API와는 별개의 서버).

//web 플랫폼 판단용
import 'package:flutter/foundation.dart';
//os 플랫폼(Android, iOS , Windows 등 ) 감지 라이브러리
import 'dart:io' show Platform;

class ApiClient {
  // ⚠️ project_4를 실제 배포하신 뒤, 아래 두 값을 배포 도메인으로 바꿔주세요.
  // (project_3의 bookproject3.duckdns.org와는 다른 도메인/포트 사용)
  static const String _deployedBaseUrl = 'https://bookproject4.duckdns.org';
  static const String _localBaseUrl = 'http://localhost:8082';

  static String getBaseUrl() {
    // 1. 웹브라우저 실행 시 - 로컬 개발 기본값
    if (kIsWeb) return _localBaseUrl;
    try {
      // 2. Android 에뮬레이터/실기기에서는 배포된 서버로 접속
      if (Platform.isAndroid) return _deployedBaseUrl;
    } catch (_) {}
    // 3. Windows 데스크톱 네이티브 앱 실행 시 - 로컬 개발 기본값
    return _localBaseUrl;
  }
}
