import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../data/auth_provider.dart';

/// 회원가입 - 서버 규칙 그대로: 이메일 인증번호(5분 유효) 확인 → 이메일/닉네임 중복확인 → 가입
/// (예전 앱은 인증 단계가 없어서 서버가 항상 가입을 거절했음)
class SignupPage extends ConsumerStatefulWidget {
  const SignupPage({super.key});

  @override
  ConsumerState<SignupPage> createState() => _SignupPageState();
}

class _SignupPageState extends ConsumerState<SignupPage> {
  final _email = TextEditingController();
  final _code = TextEditingController();
  final _password = TextEditingController();
  final _password2 = TextEditingController();
  final _nickname = TextEditingController();

  XFile? _profile;
  Uint8List? _profilePreview;
  bool _codeSent = false;
  bool _verified = false;
  bool _busy = false;
  int _remain = 0;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    for (final c in [_email, _code, _password, _password2, _nickname]) {
      c.dispose();
    }
    super.dispose();
  }

  void _toast(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _sendCode() async {
    final String email = _email.text.trim();
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) return _toast('이메일 형식을 확인해 주세요.');
    setState(() => _busy = true);
    final AuthNotifier auth = ref.read(authProvider.notifier);
    try {
      if (await auth.isEmailTaken(email)) {
        setState(() => _busy = false);
        return _toast('이미 가입된 이메일입니다.');
      }
    } catch (_) {}
    final String? err = await auth.sendEmailCode(email);
    if (!mounted) return;
    setState(() => _busy = false);
    if (err != null) return _toast(err);
    setState(() {
      _codeSent = true;
      _verified = false;
      _remain = 300;
    });
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (!mounted) return t.cancel();
      setState(() => _remain = _remain > 0 ? _remain - 1 : 0);
      if (_remain == 0) t.cancel();
    });
    _toast('인증번호를 보냈습니다. 메일함을 확인해 주세요.');
  }

  Future<void> _verify() async {
    final String? err = await ref.read(authProvider.notifier).verifyEmailCode(_email.text.trim(), _code.text.trim());
    if (!mounted) return;
    if (err != null) return _toast(err);
    _timer?.cancel();
    setState(() => _verified = true);
    _toast('이메일 인증이 완료되었습니다.');
  }

  Future<void> _submit() async {
    if (!_verified) return _toast('이메일 인증을 먼저 완료해 주세요.');
    final String nickname = _nickname.text.trim();
    if (_password.text.length < 8) return _toast('비밀번호는 8자 이상 입력해 주세요.');
    if (_password.text != _password2.text) return _toast('비밀번호가 서로 다릅니다.');
    if (nickname.isEmpty || nickname.length > 20) return _toast('닉네임은 1~20자로 입력해 주세요.');
    setState(() => _busy = true);
    final AuthNotifier auth = ref.read(authProvider.notifier);
    try {
      if (await auth.isNicknameTaken(nickname)) {
        setState(() => _busy = false);
        return _toast('이미 사용 중인 닉네임입니다.');
      }
    } catch (_) {}
    final String? err = await auth.signup(_email.text.trim(), _password.text, nickname, profileImage: _profile);
    if (!mounted) return;
    setState(() => _busy = false);
    if (err != null) return _toast(err);
    _toast('회원가입이 완료되었습니다. 로그인해 주세요.');
    Navigator.pushReplacementNamed(context, '/login');
  }

  Future<void> _pickProfile() async {
    final XFile? f = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 800, imageQuality: 85);
    if (f == null) return;
    final Uint8List bytes = await f.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) return _toast('프로필 사진은 5MB 이하만 가능해요.');
    setState(() {
      _profile = f;
      _profilePreview = bytes;
    });
  }

  @override
  Widget build(BuildContext context) {
    final String mmss = '${(_remain ~/ 60).toString().padLeft(2, '0')}:${(_remain % 60).toString().padLeft(2, '0')}';
    return Scaffold(
      appBar: AppBar(title: const Text('회원가입')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const Text('1. 이메일 인증', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Row(children: [
              Expanded(
                child: TextField(
                  controller: _email,
                  enabled: !_verified,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(labelText: '이메일', border: OutlineInputBorder()),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton(onPressed: _busy || _verified ? null : _sendCode, child: Text(_codeSent ? '재전송' : '인증번호')),
            ]),
            if (_codeSent && !_verified) ...[
              const SizedBox(height: 8),
              Row(children: [
                Expanded(
                  child: TextField(
                    controller: _code,
                    keyboardType: TextInputType.number,
                    maxLength: 6,
                    decoration: InputDecoration(labelText: '인증번호 6자리', border: const OutlineInputBorder(), suffixText: mmss, counterText: ''),
                  ),
                ),
                const SizedBox(width: 8),
                FilledButton(onPressed: _remain > 0 ? _verify : null, child: const Text('확인')),
              ]),
            ],
            if (_verified)
              const Padding(
                padding: EdgeInsets.only(top: 6),
                child: Text('✓ 인증 완료 (30분 안에 가입을 마쳐 주세요)', style: TextStyle(color: Colors.green)),
              ),
            const SizedBox(height: 24),
            const Text('2. 계정 정보', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(controller: _password, obscureText: true, decoration: const InputDecoration(labelText: '비밀번호 (8자 이상)', border: OutlineInputBorder())),
            const SizedBox(height: 10),
            TextField(controller: _password2, obscureText: true, decoration: const InputDecoration(labelText: '비밀번호 확인', border: OutlineInputBorder())),
            const SizedBox(height: 10),
            TextField(controller: _nickname, maxLength: 20, decoration: const InputDecoration(labelText: '닉네임', border: OutlineInputBorder())),
            const Text('프로필 이미지 (선택)', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Row(children: [
              CircleAvatar(
                radius: 30,
                backgroundImage: _profilePreview != null ? MemoryImage(_profilePreview!) : null,
                child: _profilePreview == null ? const Icon(Icons.person, size: 30) : null,
              ),
              const SizedBox(width: 12),
              OutlinedButton.icon(onPressed: _pickProfile, icon: const Icon(Icons.upload), label: const Text('이미지 선택')),
              if (_profile != null)
                IconButton(onPressed: () => setState(() { _profile = null; _profilePreview = null; }), icon: const Icon(Icons.close)),
            ]),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _busy ? null : _submit,
              style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
              child: const Text('가입하기', style: TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }
}
