import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

import '../../core/network/api_client.dart';

/// 다음(카카오) 우편번호 검색 - 서버의 /postcode.html 을 WebView 로 열고,
/// 주소를 고르면 JavaScriptChannel(PostcodeChannel)로 {zonecode, address, extra} 를 돌려받는다.
/// Navigator.pop 결과: `Map<String, String>`
class PostcodePage extends StatefulWidget {
  const PostcodePage({super.key});

  @override
  State<PostcodePage> createState() => _PostcodePageState();
}

class _PostcodePageState extends State<PostcodePage> {
  late final WebViewController _controller;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..addJavaScriptChannel('PostcodeChannel', onMessageReceived: (JavaScriptMessage msg) {
        final Map<String, dynamic> data = jsonDecode(msg.message) as Map<String, dynamic>;
        Navigator.pop(context, data.map((k, v) => MapEntry(k, v?.toString() ?? '')));
      })
      ..setNavigationDelegate(NavigationDelegate(onPageFinished: (_) {
        if (mounted) setState(() => _loading = false);
      }))
      ..loadRequest(Uri.parse('${ApiClient.getBaseUrl()}/postcode.html'));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('우편번호 검색')),
      body: Stack(children: [
        WebViewWidget(controller: _controller),
        if (_loading) const Center(child: CircularProgressIndicator()),
      ]),
    );
  }
}
