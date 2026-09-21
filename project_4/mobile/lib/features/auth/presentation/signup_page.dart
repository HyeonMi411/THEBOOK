import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/auth_provider.dart';

class SignupPage extends ConsumerStatefulWidget {
  const SignupPage({super.key});

  @override
  ConsumerState<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends ConsumerState<SignupPage> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _nicknameController = TextEditingController();

  bool _isEmailChecked = false;
  bool _isNicknameChecked = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _nicknameController.dispose();
    super.dispose();
  }

  void _checkEmail() async {
    final email = _emailController.text.trim();
    if (email.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('이메일을 입력해주세요.')));
      return;
    }

    final exists = await ref.read(authProvider.notifier).checkEmailDuplicate(email);
    if (exists) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('이미 사용 중인 이메일입니다.')));
      // [핵심] Flutter의 local state 변경 및 UI 트리 재렌더링 트리거 함수
      setState(() => _isEmailChecked = false);
    } else {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('사용 가능한 이메일입니다.')));
      setState(() => _isEmailChecked = true);
    }
  }

  void _checkNickname() async {
    final nickname = _nicknameController.text.trim();
    if (nickname.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('닉네임을 입력해주세요.')));
      return;
    }

    final exists = await ref.read(authProvider.notifier).checkNicknameDuplicate(nickname);
    if (exists) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('이미 사용 중인 닉네임입니다.')));
      setState(() => _isNicknameChecked = false);
    } else {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('사용 가능한 닉네임입니다.')));
      setState(() => _isNicknameChecked = true);
    }
  }

  void _handleSignup() async {
    if (!_isEmailChecked) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('이메일 중복 확인을 해주세요.')));
      return;
    }
    if (!_isNicknameChecked) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('닉네임 중복 확인을 해주세요.')));
      return;
    }

    final data = {
      'email': _emailController.text.trim(),
      'password': _passwordController.text.trim(),
      'nickname': _nicknameController.text.trim(),
    };

    final success = await ref.read(authProvider.notifier).signup(data);
    if (success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('회원가입 완료! 로그인해주세요.')),
      );
      // [핵심] 현재 페이지를 스택에서 제거하고 이전 페이지로 복귀 (React Router의 router.back())
      Navigator.pop(context); 
    }
  }
  /////////////////////////////////////////////////////////////////////////////////////////////////
  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('회원가입')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            Row(
              children: [
                // [핵심] Flex Layout 내에서 입력창이 남은 공간을 차지하도록 확장해 주는 위젯
                Expanded(
                  child: TextField(
                    controller: _emailController, 
                    decoration: const InputDecoration(labelText: '이메일'),
                    onChanged: (_) => setState(() => _isEmailChecked = false),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(onPressed: _checkEmail, child: const Text('중복확인')),
              ],
            ),
            const SizedBox(height: 12),
            TextField(controller: _passwordController, obscureText: true, decoration: const InputDecoration(labelText: '비밀번호')),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _nicknameController, 
                    decoration: const InputDecoration(labelText: '닉네임'),
                    onChanged: (_) => setState(() => _isNicknameChecked = false),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(onPressed: _checkNickname, child: const Text('중복확인')),
              ],
            ),
            const SizedBox(height: 16),
            if (authState.error != null)
              Text(authState.error!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: authState.loading ? null : _handleSignup,
                child: authState.loading 
                    ? const CircularProgressIndicator(color: Colors.white) 
                    : const Text('가입하기'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
