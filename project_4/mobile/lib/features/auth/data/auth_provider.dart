import 'package:flutter_riverpod/flutter_riverpod.dart';  // 리덕스
import 'package:dio/dio.dart';  // axios  역할의 비동기 http 통신
import 'package:flutter_secure_storage/flutter_secure_storage.dart';  // jwt 저장라이브러리
import '../../../core/network/api_client.dart';

class AuthState {
  final Map<String, dynamic>? user; // 서버에서 받은 유저정보 dto객체
  final String? accessToken;  // jwt access Token
  final bool loading;  // 로딩중?
  final String? error; // 에러

  const AuthState({
    this.user,
    this.accessToken,
    this.loading = false,
    this.error,
  });
}

// [핵심] 최신 Riverpod 표준 Notifier 클래스 상속
class AuthNotifier extends Notifier<AuthState> {
  @override
  AuthState build() {
    _initDio();   // Provider 생성시 인터셉터 설정 초기화 
    return const AuthState();  // 초가회 상태 반환
  }

  late final Dio _dio;  // late (나중에-사용하기 직전에 초기화)  , final 변경 x
  // OS 암호화 저장소 객체 생성 (localStorage 대신 모바일 보안 영역 사용)
  final _storage = const FlutterSecureStorage();  // os 암호화 로컬저장소 

  void _initDio() {
    _dio = Dio(BaseOptions(
      baseUrl: ApiClient.getBaseUrl(),  // 부품객체 : Apiclient  http://localhost:8080
      headers: {'Content-Type': 'application/json'},
    ));

    // [핵심] Dio Interceptor 설정 (Axios interceptor와 100% 동일)
    _dio.interceptors.add(InterceptorsWrapper(
      // 매 api 요청마다  SecureStorage에서 토큰 일어와서 Authorization 해서 주입
      onRequest: (options, handler) async {
        final token = await _storage.read(key: 'accessToken');
        if (token != null) {
          // Authorization 헤더에 Bearer 토큰 주입
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      // 에러응답  - 401(토큰만료) → 재발급시도
      onError: (DioException e, handler) async {
        // [핵심] HTTP 401 Unauthorized 감지 시 토큰 재발급 후 원래 요청 재시도
        if (e.response?.statusCode == 401) {
          final success = await _refreshAccessToken();
          if (success) {
            final token = await _storage.read(key: 'accessToken');
            e.requestOptions.headers['Authorization'] = 'Bearer $token';
            // 기존 실패했던 API 요청 재전송
            final clonedRequest = await _dio.fetch(e.requestOptions); // 원래요청 재전송
            return handler.resolve(clonedRequest);
          }
        }
        return handler.next(e);
      },
    ));
  }
  // jwt 토큰 재발급 비동기 로직
  Future<bool> _refreshAccessToken() async {
    try {
      final response = await _dio.post('/auth/refresh');  // boot 요청경로
      final newAccessToken = response.data['accessToken'];
      if (newAccessToken != null) {
        await _storage.write(key: 'accessToken', value: newAccessToken);
        state = AuthState(
          user: state.user,
          accessToken: newAccessToken,
          loading: state.loading,
          error: state.error,
        );
        return true;
      }
    } catch (_) {
      await logout();   // refresh 실패시 강제로그아웃
    }
    return false;
  }
  // 로그인
  Future<bool> login(Map<String, dynamic> credentials) async {
    // 로딩상태시작
    state = AuthState(
      user: state.user,
      accessToken: state.accessToken,
      loading: true,
      error: null,
    );
    
    try {
      final response = await _dio.post('/auth/login', data: credentials);
      final accessToken = response.data['accessToken'];
      final user = response.data['user'];

      if (user != null && accessToken != null) {
        await _storage.write(key: 'accessToken', value: accessToken);
        state = AuthState(
          user: user,
          accessToken: accessToken,
          loading: false,
          error: null,
        );
        return true;
      } else {
        state = AuthState(
          user: state.user,
          accessToken: state.accessToken,
          loading: false,
          error: '아이디 또는 비밀번호가 올바르지 않습니다.',
        );
        return false;
      }
    } catch (err) {
      state = AuthState(
        user: state.user,
        accessToken: state.accessToken,
        loading: false,
        error: '로그인 실패: ${err.toString()}',
      );
      return false;
    }
  }
  // 로그아웃 액션
  Future<void> logout() async {
    try {
      await _dio.post('/auth/logout');  
    } catch (_) {}
    await _storage.delete(key: 'accessToken');  // 저장된 토큰삭제
    state = const AuthState();  // 상태초기화
  }

  // 회원가입
  Future<bool> signup(Map<String, dynamic> data) async {
    state = AuthState(
      user: state.user,
      accessToken: state.accessToken,
      loading: true,
      error: null,
    );

    try {
      // Spring Boot @RequestPart 멀티파트 수신 대응용 FormData 객체 생성
      final formData = FormData.fromMap({
        'email': data['email'],
        'password': data['password'],
        'nickname': data['nickname'],
      });

      await _dio.post(
        '/auth/signup', 
        data: formData,
        options: Options(headers: {'Content-Type': 'multipart/form-data'}),
      );

      state = AuthState(
        user: state.user,
        accessToken: state.accessToken,
        loading: false,
        error: null,
      );
      return true;
    } catch (err) {
      state = AuthState(
        user: state.user,
        accessToken: state.accessToken,
        loading: false,
        error: '회원가입 실패: ${err.toString()}',
      );
      return false;
    }
  }
  // 이메일 중복체크 api  ( GET   /auth/check-email )
  Future<bool> checkEmailDuplicate(String email) async {
    try {
      final response = await _dio.get('/auth/check-email', queryParameters: {'email': email});
      return response.data;
    } catch (e) {
      return false;
    }
  }
  // 닉네임 중복체크 api  ( GET   /auth/check-nickname )
  Future<bool> checkNicknameDuplicate(String nickname) async {
    try {
      final response = await _dio.get('/auth/check-nickname', queryParameters: {'nickname': nickname});
      return response.data;  // true:이미존재, false: 사용가능
    } catch (e) {
      return false;
    }
  }
}
// [핵심] Riverpod NotifierProvider 등록
final authProvider = NotifierProvider<AuthNotifier, AuthState>(() {
  return AuthNotifier();
});

//  React [Redux + Saga + api/axios.js(jwt 토큰처리)]
//  AuthState    - Redux
//  _dio.interceptors :  api 요청마다 `Bearer ${token}` 을 헤더주입 , 401 에러토큰
