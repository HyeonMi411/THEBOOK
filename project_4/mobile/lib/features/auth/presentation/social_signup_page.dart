import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/auth_provider.dart';

/// 신규 소셜회원 가입확인 - 서버 정책상 소셜 가입도 이메일 인증번호 확인이 필요 (UserController.socialSignupComplete)
class SocialSignupPage extends ConsumerStatefulWidget {
  final String signupToken;
  const SocialSignupPage({super.key, required this.signupToken});

  @override
  ConsumerState<SocialSignupPage> createState() => _SocialSignupPageState();
}

class _SocialSignupPageState extends ConsumerState<SocialSignupPage> {
  final _nickname = TextEditingController();
  final _code = TextEditingController();
  String? _email;
  String? _provider;
  bool _loading = true;
  bool _codeSent = false;
  bool _verified = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nickname.dispose();
    _code.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final Map<String, dynamic>? p = await ref.read(authProvider.notifier).socialPreview(widget.signupToken);
    if (!mounted) return;
    setState(() {
      _loading = false;
      _email = p?['email']?.toString();
      _provider = p?['provider']?.toString();
      _nickname.text = p?['nicknameSuggestion']?.toString() ?? '';
    });
  }

  void _toast(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _send() async {
    final String? err = await ref.read(authProvider.notifier).sendEmailCode(_email!);
    if (!mounted) return;
    if (err != null) return _toast(err);
    setState(() => _codeSent = true);
    _toast('$_email 로 인증번호를 보냈습니다.');
  }

  Future<void> _verify() async {
    final String? err = await ref.read(authProvider.notifier).verifyEmailCode(_email!, _code.text.trim());
    if (!mounted) return;
    if (err != null) return _toast(err);
    setState(() => _verified = true);
  }

  Future<void> _complete() async {
    final String nick = _nickname.text.trim();
    if (nick.isEmpty) return _toast('닉네임을 입력해 주세요.');
    final String? err = await ref.read(authProvider.notifier).completeSocialSignup(widget.signupToken, nick);
    if (!mounted) return;
    if (err != null) return _toast(err);
    _toast('가입이 완료되었습니다. 환영합니다!');
    Navigator.popUntil(context, (r) => r.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('소셜 회원가입 확인')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _email == null
              ? const Center(child: Text('가입 정보가 만료되었습니다. 다시 로그인해 주세요.'))
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    Text('${_provider ?? '소셜'} 계정으로 처음 오셨네요!', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 16),
                    ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.email_outlined), title: Text(_email!)),
                    if (!_verified) ...[
                      Row(children: [
                        Expanded(
                          child: TextField(
                            controller: _code,
                            enabled: _codeSent,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(labelText: '이메일 인증번호', border: OutlineInputBorder()),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _codeSent
                            ? FilledButton(onPressed: _verify, child: const Text('확인'))
                            : OutlinedButton(onPressed: _send, child: const Text('인증번호')),
                      ]),
                    ] else
                      const Text('✓ 이메일 인증 완료', style: TextStyle(color: Colors.green)),
                    const SizedBox(height: 16),
                    TextField(controller: _nickname, maxLength: 20, decoration: const InputDecoration(labelText: '사용할 닉네임', border: OutlineInputBorder())),
                    const SizedBox(height: 12),
                    FilledButton(onPressed: _verified ? _complete : null, child: const Text('가입 완료')),
                  ],
                ),
    );
  }
}
