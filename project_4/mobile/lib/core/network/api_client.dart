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

  // 빌드/실행 시 --dart-define=API_BASE_URL=... 로 서버 주소를 지정하면 그 값을 최우선으로 사용합니다.
  //  - 에뮬레이터 로컬 개발 : flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8082
  //  - 배포 APK 빌드        : GitHub Actions(deploy-project4.yml)가 API_BASE_URL_P4 시크릿으로 주입
  static const String _envBaseUrl = String.fromEnvironment('API_BASE_URL');

  static String getBaseUrl() {
    // 0. --dart-define 으로 지정된 주소가 있으면 그대로 사용
    if (_envBaseUrl.isNotEmpty) return _envBaseUrl;
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
