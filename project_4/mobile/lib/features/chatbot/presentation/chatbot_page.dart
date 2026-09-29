import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/dio_client.dart';

/// 대화 한 줄
class _Msg {
  final bool fromUser;
  final String text;
  final List<Map<String, dynamic>> suggestions; // [{faqId, label}]
  final bool handoff;   // true → "이메일로 문의하기" 버튼
  final String source;  // faq | ai | none
  const _Msg(this.fromUser, this.text, {this.suggestions = const [], this.handoff = false, this.source = 'faq'});
}

/// 상담 챗봇 (로그인 없이 사용) - 버튼형 메뉴 + 직접 입력 + AI 보조 답변 + 상담원(이메일) 연결
class ChatbotPage extends StatefulWidget {
  const ChatbotPage({super.key});

  @override
  State<ChatbotPage> createState() => _ChatbotPageState();
}

class _ChatbotPageState extends State<ChatbotPage> {
  final TextEditingController _input = TextEditingController();
  final ScrollController _scroll = ScrollController();
  final List<_Msg> _messages = [];
  List<Map<String, dynamic>> _menu = [];
  String _contactEmail = '';
  bool _sending = false;

  Dio get _dio => DioClient.instance;

  @override
  void initState() {
    super.initState();
    _messages.add(const _Msg(false, '안녕하세요! BookStore 상담 챗봇입니다 😊\n아래 메뉴를 고르거나 궁금한 내용을 입력해 주세요.'));
    _loadMenu();
  }

  @override
  void dispose() {
    _input.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _loadMenu() async {
    try {
      final Response<dynamic> res = await _dio.get('/api/chatbot/menu');
      setState(() => _menu = (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList());
    } catch (_) {
      setState(() => _messages.add(const _Msg(false, '메뉴를 불러오지 못했어요. 궁금한 내용을 직접 입력해 주세요.', handoff: true)));
    }
  }

  Future<void> _ask({String? faqId, String? label, String? message}) async {
    final String shown = label ?? message ?? '';
    if (shown.trim().isEmpty || _sending) return;
    setState(() {
      _messages.add(_Msg(true, shown));
      _sending = true;
    });
    _scrollDown();
    try {
      final Response<dynamic> res = await _dio.post('/api/chatbot/ask', data: {
        'faqId': ?faqId,
        'message': ?message,
      });
      final Map<dynamic, dynamic> d = res.data as Map;
      _contactEmail = d['contactEmail']?.toString() ?? _contactEmail;
      setState(() => _messages.add(_Msg(
            false,
            d['answer']?.toString() ?? '',
            suggestions: ((d['suggestions'] as List?) ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList(),
            handoff: d['handoff'] == true,
            source: d['source']?.toString() ?? 'faq',
          )));
    } catch (e) {
      setState(() => _messages.add(_Msg(false, errorMessage(e, fallback: '잠시 후 다시 시도해 주세요.'), handoff: true, source: 'none')));
    } finally {
      setState(() => _sending = false);
      _scrollDown();
    }
  }

  void _scrollDown() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(_scroll.position.maxScrollExtent, duration: const Duration(milliseconds: 250), curve: Curves.easeOut);
      }
    });
  }

  Future<void> _mail() async {
    final String to = _contactEmail.isEmpty ? 'jewelkhm@naver.com' : _contactEmail;
    // queryParameters 를 쓰면 공백이 '+' 로 바뀌어 메일 제목이 깨지므로 직접 인코딩
    final Uri uri = Uri.parse('mailto:$to?subject=${Uri.encodeComponent('[BookStore 앱 문의]')}');
    if (!await launchUrl(uri) && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('메일 앱을 열 수 없습니다. $to 로 문의해 주세요.')));
    }
  }

  void _showMenu() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.6,
        builder: (_, sc) => ListView(controller: sc, children: [
          for (final Map<String, dynamic> c in _menu)
            ExpansionTile(
              title: Text(c['label'].toString(), style: const TextStyle(fontWeight: FontWeight.bold)),
              children: [
                for (final dynamic it in (c['items'] as List? ?? []))
                  ListTile(
                    title: Text(it['label'].toString()),
                    onTap: () {
                      Navigator.pop(ctx);
                      _ask(faqId: it['faqId'].toString(), label: it['label'].toString());
                    },
                  ),
              ],
            ),
        ]),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('상담 챗봇'),
        actions: [IconButton(icon: const Icon(Icons.list_alt), tooltip: '전체 메뉴', onPressed: _menu.isEmpty ? null : _showMenu)],
      ),
      body: Column(children: [
        Expanded(
          child: ListView.builder(
            controller: _scroll,
            padding: const EdgeInsets.all(12),
            itemCount: _messages.length + (_messages.length == 1 && _menu.isNotEmpty ? 1 : 0),
            itemBuilder: (_, i) => i < _messages.length ? _bubble(_messages[i]) : _categoryChips(),
          ),
        ),
        if (_sending) const LinearProgressIndicator(minHeight: 2),
        SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(12, 6, 6, 8),
            child: Row(children: [
              Expanded(
                child: TextField(
                  controller: _input,
                  maxLength: 200,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => _send(),
                  decoration: InputDecoration(
                    hintText: '예) 결제 완료 문자가 안 와요',
                    counterText: '',
                    isDense: true,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(24)),
                  ),
                ),
              ),
              IconButton(icon: const Icon(Icons.send), onPressed: _sending ? null : _send),
            ]),
          ),
        ),
      ]),
    );
  }

  void _send() {
    final String text = _input.text.trim();
    if (text.isEmpty) return;
    _input.clear();
    _ask(message: text);
  }

  Widget _categoryChips() {
    return Wrap(spacing: 6, runSpacing: 6, children: [
      for (final Map<String, dynamic> c in _menu)
        for (final dynamic it in (c['items'] as List? ?? []).take(2))
          ActionChip(label: Text(it['label'].toString()), onPressed: () => _ask(faqId: it['faqId'].toString(), label: it['label'].toString())),
    ]);
  }

  Widget _bubble(_Msg m) {
    final Color bg = m.fromUser ? Colors.blue.shade600 : Colors.grey.shade100;
    return Align(
      alignment: m.fromUser ? Alignment.centerRight : Alignment.centerLeft,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.8),
        child: Column(crossAxisAlignment: m.fromUser ? CrossAxisAlignment.end : CrossAxisAlignment.start, children: [
          if (!m.fromUser && m.source == 'ai')
            Padding(
              padding: const EdgeInsets.only(top: 4, left: 4),
              child: Text('🤖 AI 보조 답변 · 정확한 안내는 상담원 문의를 이용해 주세요', style: TextStyle(fontSize: 11, color: Colors.purple.shade400)),
            ),
          Container(
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(14)),
            child: Text(m.text, style: TextStyle(color: m.fromUser ? Colors.white : Colors.black87, height: 1.5)),
          ),
          if (m.suggestions.isNotEmpty)
            Wrap(spacing: 6, runSpacing: 4, children: [
              for (final Map<String, dynamic> s in m.suggestions)
                ActionChip(
                  label: Text(s['label'].toString(), style: const TextStyle(fontSize: 12)),
                  onPressed: () => _ask(faqId: s['faqId'].toString(), label: s['label'].toString()),
                ),
            ]),
          if (m.handoff)
            TextButton.icon(onPressed: _mail, icon: const Icon(Icons.email_outlined, size: 18), label: const Text('상담원에게 이메일 문의')),
        ]),
      ),
    );
  }
}
