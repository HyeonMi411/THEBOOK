import 'package:flutter/material.dart';

/// 한 줄 입력 다이얼로그 (재고 수량 / 닉네임 / 판매가 등)
///
/// 입력칸의 TextEditingController 를 **다이얼로그 위젯이 직접 만들고 정리(dispose)** 한다.
/// 예전처럼 showDialog 가 끝나자마자 바깥에서 dispose 하면, 다이얼로그가 닫히는 애니메이션 동안
/// 입력칸이 이미 정리된 컨트롤러를 사용해서
/// "A TextEditingController was used after being disposed" → "_dependents.isEmpty" 빨간 화면이 난다.
///
/// 반환: 확인을 누르면 앞뒤 공백을 뺀 입력값, 취소/바깥 터치면 null
Future<String?> showTextInputDialog(
  BuildContext context, {
  required String title,
  String initialValue = '',
  String? label,
  String? hint,
  String? suffix,
  String? helper,
  Widget? header,
  TextInputType? keyboardType,
  int? maxLength,
  String confirmText = '확인',
}) {
  return showDialog<String>(
    context: context,
    builder: (_) => _TextInputDialog(
      title: title,
      initialValue: initialValue,
      label: label,
      hint: hint,
      suffix: suffix,
      helper: helper,
      header: header,
      keyboardType: keyboardType,
      maxLength: maxLength,
      confirmText: confirmText,
    ),
  );
}

class _TextInputDialog extends StatefulWidget {
  final String title;
  final String initialValue;
  final String? label;
  final String? hint;
  final String? suffix;
  final String? helper;
  final Widget? header;
  final TextInputType? keyboardType;
  final int? maxLength;
  final String confirmText;

  const _TextInputDialog({
    required this.title,
    required this.initialValue,
    this.label,
    this.hint,
    this.suffix,
    this.helper,
    this.header,
    this.keyboardType,
    this.maxLength,
    required this.confirmText,
  });

  @override
  State<_TextInputDialog> createState() => _TextInputDialogState();
}

class _TextInputDialogState extends State<_TextInputDialog> {
  late final TextEditingController _controller = TextEditingController(text: widget.initialValue);

  @override
  void dispose() {
    _controller.dispose(); // 다이얼로그가 완전히 사라질 때(애니메이션 종료 후) 정리됨
    super.dispose();
  }

  void _submit() => Navigator.pop(context, _controller.text.trim());

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.title),
      content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (widget.header != null) ...[widget.header!, const SizedBox(height: 12)],
        TextField(
          controller: _controller,
          autofocus: true,
          keyboardType: widget.keyboardType,
          maxLength: widget.maxLength,
          onSubmitted: (_) => _submit(),
          decoration: InputDecoration(
            labelText: widget.label,
            hintText: widget.hint,
            suffixText: widget.suffix,
            border: const OutlineInputBorder(),
          ),
        ),
        if (widget.helper != null) ...[
          const SizedBox(height: 6),
          Text(widget.helper!, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        ],
      ]),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('취소')),
        FilledButton(onPressed: _submit, child: Text(widget.confirmText)),
      ],
    );
  }
}
