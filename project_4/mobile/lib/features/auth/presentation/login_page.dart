import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/auth_provider.dart';

// React의 useState + useSelector 기능을 모두 사용하기 위해 상속받는 클래스
class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  // [핵심] Flutter의 폼 입력 제어 컨트롤러 (React의 useState/useRef 역할)
  // _변수  : 해당변수를 클래스 내부에서만 접근
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    // [핵심] 메모리 누수 방지를 위한 컨트롤러 객체 해제
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _handleLogin() async {
    // TextEditingController에서 텍스트 값 추출
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();

    //  Toast  / Alert  대신 사용하는 Flutter 표준스낵바
    if (email.isEmpty || password.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('이메일과 비밀번호를 입력해주세요.')),
      );
      return;
    }
    // [핵심] Notifier의 메서드를 호출(dispatch)하기 위해 ref.read().notifier 사용
    final success = await ref.read(authProvider.notifier).login({'email': email, 'password': password});
    
    if (success && mounted) {
      // [핵심] 히스토리 스택을 모두 삭제하고 이동 (React Router의 router.replace('/') 효과)
      Navigator.pushNamedAndRemoveUntil(context, '/', (route) => false );
    }
  }
  //////////////////////////////////////////////////////////////////////////////////////
  @override
  Widget build(BuildContext context) {
    // [핵심] 전역 AuthState 변화를 구독하여 UI 자동 재빌드 (Redux의 useSelector 역할)
    final authState = ref.watch(authProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('로그인')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(controller: _emailController, decoration: const InputDecoration(labelText: '이메일')),
            const SizedBox(height: 12),
            TextField(controller: _passwordController, obscureText: true, decoration: const InputDecoration(labelText: '비밀번호')),
            const SizedBox(height: 24),
            // 에러 발생시 조건부 렌더링
            if (authState.error != null)
              Text(authState.error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                // 로딩 중일때 버튼 비활성화 여부 
                onPressed: authState.loading ? null : _handleLogin,
                child: authState.loading ? const CircularProgressIndicator(color: Colors.white) : const Text('로그인'),
              ),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () {
                Navigator.pushNamed(context, '/signup');
              },
              child: const Text('계정이 없으신가요? 회원가입하기'),
            ),
          ],
        ),
      ),
    );
  }
}
