import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/network/refresh_cookie.dart';

/// 로그인 상태 (React 의 Redux store 역할)
class AuthState {
  final Map<String, dynamic>? user; // 서버의 UserResponseDto
  final bool loading;
  final bool restored; // 앱 시작 시 저장된 토큰으로 로그인 복원을 시도했는지

  const AuthState({this.user, this.loading = false, this.restored = false});

  bool get isLoggedIn => user != null;
  bool get isAdmin => user?['role'] == 'ROLE_ADMIN';
  int? get userId => user?['id'] is num ? (user!['id'] as num).toInt() : null;
  String get nickname => user?['nickname']?.toString() ?? '';

  AuthState copyWith({Map<String, dynamic>? user, bool? loading, bool? restored, bool clearUser = false}) {
    return AuthState(
      user: clearUser ? null : (user ?? this.user),
      loading: loading ?? this.loading,
      restored: restored ?? this.restored,
    );
  }
}

class AuthNotifier extends Notifier<AuthState> {
  Dio get _dio => DioClient.instance;

  @override
  AuthState build() {
    // 재발급까지 실패(=세션 만료)하면 로그인 상태를 비운다
    DioClient.onSessionExpired = () => state = state.copyWith(clearUser: true);
    return const AuthState();
  }

  /// 앱 시작 시 한 번 - 저장된 토큰(없으면 리프레시 쿠키)으로 로그인 상태 복원
  /// (예전 앱은 앱을 다시 켜면 토큰이 남아 있어도 user 가 비어 있어서 항상 로그아웃 상태로 보였다)
  Future<void> restoreSession() async {
    try {
      String? token = await DioClient.readAccessToken();
      if (token == null && await DioClient.refreshAccessToken()) {
        token = await DioClient.readAccessToken();
      }
      if (token != null) {
        final Response<dynamic> res = await _dio.get('/auth/me');
        state = state.copyWith(user: Map<String, dynamic>.from(res.data as Map), restored: true);
        return;
      }
    } catch (_) {
      await DioClient.clearTokens();
    }
    state = state.copyWith(clearUser: true, restored: true);
  }

  Future<void> _applyLoginResponse(Response<dynamic> res) async {
    await RefreshCookie.saveFromResponse(res); // Set-Cookie 의 refreshToken 보관 (모바일은 쿠키 자동저장이 없음)
    final Map<dynamic, dynamic> data = res.data as Map;
    await DioClient.saveAccessToken(data['accessToken'] as String);
    state = state.copyWith(user: Map<String, dynamic>.from(data['user'] as Map), loading: false);
  }

  /// @return 실패 시 에러 메시지, 성공 시 null
  Future<String?> login(String email, String password) async {
    state = state.copyWith(loading: true);
    try {
      final Response<dynamic> res = await _dio.post('/auth/login', data: {'email': email, 'password': password});
      await _applyLoginResponse(res);
      return null;
    } catch (e) {
      state = state.copyWith(loading: false);
      return errorMessage(e, fallback: '이메일 또는 비밀번호가 올바르지 않습니다.');
    }
  }

  /// 소셜 로그인 후 딥링크로 받은 일회용 코드 → 토큰
  Future<String?> exchangeSocialCode(String code) async {
    state = state.copyWith(loading: true);
    try {
      final Response<dynamic> res = await _dio.post('/auth/app/exchange', data: {'code': code});
      await _applyLoginResponse(res);
      return null;
    } catch (e) {
      state = state.copyWith(loading: false);
      return errorMessage(e, fallback: '소셜 로그인에 실패했습니다.');
    }
  }

  Future<Map<String, dynamic>?> socialPreview(String signupToken) async {
    try {
      final Response<dynamic> res = await _dio.get('/auth/social/preview', queryParameters: {'signupToken': signupToken});
      return Map<String, dynamic>.from(res.data as Map);
    } catch (_) {
      return null;
    }
  }

  Future<String?> completeSocialSignup(String signupToken, String nickname) async {
    try {
      final Response<dynamic> res =
          await _dio.post('/auth/social/signup', data: {'signupToken': signupToken, 'nickname': nickname});
      await _applyLoginResponse(res);
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '가입을 완료하지 못했습니다.');
    }
  }

  // ── 회원가입 (이메일 인증 → 중복확인 → 가입) ──

  Future<String?> sendEmailCode(String email) async {
    try {
      await _dio.post('/auth/email/send-code', queryParameters: {'email': email});
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '인증번호를 보내지 못했습니다.');
    }
  }

  Future<String?> verifyEmailCode(String email, String code) async {
    try {
      await _dio.post('/auth/email/verify-code', queryParameters: {'email': email, 'code': code});
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '인증번호가 올바르지 않습니다.');
    }
  }

  /// true = 이미 사용 중
  Future<bool> isEmailTaken(String email) async {
    final Response<dynamic> res = await _dio.get('/auth/check-email', queryParameters: {'email': email});
    return res.data == true;
  }

  Future<bool> isNicknameTaken(String nickname) async {
    final Response<dynamic> res = await _dio.get('/auth/check-nickname', queryParameters: {'nickname': nickname});
    return res.data == true;
  }

  /// profileImage 는 선택 (서버 multipart 파트명 ufile)
  Future<String?> signup(String email, String password, String nickname, {XFile? profileImage}) async {
    try {
      await _dio.post('/auth/signup',
          data: FormData.fromMap({
            'email': email,
            'password': password,
            'nickname': nickname,
            if (profileImage != null)
              'ufile': MultipartFile.fromBytes(await profileImage.readAsBytes(), filename: profileImage.name),
          }));
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '회원가입에 실패했습니다.');
    }
  }

  // ── 마이페이지 ──

  Future<String?> updateNickname(String nickname) async {
    final int? id = state.userId;
    if (id == null) return '로그인이 필요합니다.';
    try {
      final Response<dynamic> res = await _dio.patch('/auth/$id/nickname', queryParameters: {'nickname': nickname});
      state = state.copyWith(user: Map<String, dynamic>.from(res.data as Map));
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '닉네임을 바꾸지 못했습니다.');
    }
  }

  Future<String?> updateProfileImage(XFile file) async {
    final int? id = state.userId;
    if (id == null) return '로그인이 필요합니다.';
    try {
      final FormData form = FormData.fromMap({
        'ufile': MultipartFile.fromBytes(await file.readAsBytes(), filename: file.name),
      });
      final Response<dynamic> res = await _dio.patch('/auth/$id/profile-image', data: form);
      state = state.copyWith(user: Map<String, dynamic>.from(res.data as Map));
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '프로필 사진을 바꾸지 못했습니다.');
    }
  }

  Future<String?> withdraw() async {
    try {
      await _dio.delete('/auth/me', options: await RefreshCookie.cookieOptions());
      await DioClient.clearTokens();
      state = state.copyWith(clearUser: true);
      return null;
    } catch (e) {
      return errorMessage(e, fallback: '탈퇴 처리에 실패했습니다.');
    }
  }

  Future<void> logout() async {
    try {
      await _dio.post('/auth/logout', options: await RefreshCookie.cookieOptions());
    } catch (_) {}
    await DioClient.clearTokens();
    state = state.copyWith(clearUser: true);
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthState>(AuthNotifier.new);
