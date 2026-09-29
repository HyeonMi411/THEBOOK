import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/api_client.dart';
import '../data/auth_provider.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    final String? err = await ref.read(authProvider.notifier).login(_email.text.trim(), _password.text);
    if (!mounted) return;
    if (err != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
      return;
    }
    Navigator.pop(context);
  }

  /// 소셜 로그인 - 외부 브라우저에서 로그인하면 서버가 bookstore4:// 딥링크로 앱에 돌려준다 (app.dart 에서 처리)
  Future<void> _social(String provider) async {
    final Uri uri = Uri.parse('${ApiClient.getBaseUrl()}/oauth2/authorization/$provider');
    if (!await launchUrl(uri, mode: LaunchMode.externalApplication) && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('브라우저를 열 수 없습니다.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool loading = ref.watch(authProvider).loading;
    return Scaffold(
      appBar: AppBar(title: const Text('로그인')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 12),
                Image.asset('assets/images/app_logo.png', height: 72, errorBuilder: (_, _, _) => const SizedBox()),
                const SizedBox(height: 24),
                TextFormField(
                  controller: _email,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(labelText: '이메일', border: OutlineInputBorder()),
                  validator: (v) => (v == null || !v.contains('@')) ? '이메일을 입력해 주세요.' : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _password,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: '비밀번호', border: OutlineInputBorder()),
                  validator: (v) => (v == null || v.isEmpty) ? '비밀번호를 입력해 주세요.' : null,
                  onFieldSubmitted: (_) => _login(),
                ),
                const SizedBox(height: 20),
                FilledButton(
                  onPressed: loading ? null : _login,
                  style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                  child: loading
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('로그인', style: TextStyle(fontSize: 16)),
                ),
                TextButton(
                  onPressed: () => Navigator.pushReplacementNamed(context, '/signup'),
                  child: const Text('아직 회원이 아니신가요? 회원가입'),
                ),
                const SizedBox(height: 16),
                const Row(children: [
                  Expanded(child: Divider()),
                  Padding(padding: EdgeInsets.symmetric(horizontal: 8), child: Text('간편 로그인', style: TextStyle(color: Colors.grey))),
                  Expanded(child: Divider()),
                ]),
                const SizedBox(height: 12),
                _SocialButton(label: '카카오로 로그인', color: const Color(0xFFFEE500), textColor: Colors.black87, onTap: () => _social('kakao')),
                _SocialButton(label: '네이버로 로그인', color: const Color(0xFF03C75A), textColor: Colors.white, onTap: () => _social('naver')),
                _SocialButton(label: '구글로 로그인', color: Colors.white, textColor: Colors.black87, onTap: () => _social('google'), border: true),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SocialButton extends StatelessWidget {
  final String label;
  final Color color;
  final Color textColor;
  final VoidCallback onTap;
  final bool border;

  const _SocialButton({required this.label, required this.color, required this.textColor, required this.onTap, this.border = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: ElevatedButton(
        onPressed: onTap,
        style: ElevatedButton.styleFrom(
          backgroundColor: color,
          foregroundColor: textColor,
          padding: const EdgeInsets.symmetric(vertical: 13),
          side: border ? const BorderSide(color: Colors.black26) : null,
        ),
        child: Text(label),
      ),
    );
  }
}
