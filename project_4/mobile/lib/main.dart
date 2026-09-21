// flutter 기본 ui 컴포넌트 라이브러리 임포트 
import 'package:flutter/material.dart';
// Riverpod 전역상태관리 라이브러리 임포트
import 'package:flutter_riverpod/flutter_riverpod.dart';
// 메인앱설정
import 'app.dart';

// 앱실행 진입점
void main() {
  // Flutter 엔진과 플랫폼 바인딩 초기화
  WidgetsFlutterBinding.ensureInitialized();  
  // ProviderScope: Riverpod 전역 상태를 앱 전체에 주입 (Redux의 <Provider> 역할)
  runApp(const ProviderScope(child: App()));
}