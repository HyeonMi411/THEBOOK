import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../core/network/dio_client.dart';
import '../../core/utils/format.dart';
import '../../shared/app_layout.dart';
import '../home/data/home_provider.dart';

/// 공지사항 등록 / 수정 (관리자) - 3차 /notices/new, 공지 수정 모달과 같은 항목 (제목·내용·첨부 이미지)
class NoticeFormPage extends ConsumerStatefulWidget {
  final Map<String, dynamic>? editing;
  const NoticeFormPage({super.key, this.editing});

  @override
  ConsumerState<NoticeFormPage> createState() => _NoticeFormPageState();
}

class _NoticeFormPageState extends ConsumerState<NoticeFormPage> {
  late final TextEditingController _title = TextEditingController(text: widget.editing?['btitle']?.toString() ?? '');
  late final TextEditingController _content = TextEditingController(text: widget.editing?['bcontent']?.toString() ?? '');
  XFile? _file;
  Uint8List? _preview;
  bool _busy = false;

  bool get _isEdit => widget.editing != null;

  @override
  void dispose() {
    _title.dispose();
    _content.dispose();
    super.dispose();
  }

  Future<void> _pick() async {
    final XFile? f = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 1600, imageQuality: 85);
    if (f == null) return;
    final Uint8List bytes = await f.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('첨부 이미지는 5MB 이하만 가능해요.')));
      return;
    }
    setState(() {
      _file = f;
      _preview = bytes;
    });
  }

  Future<void> _submit() async {
    if (_title.text.trim().isEmpty || _content.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('제목과 내용을 입력해 주세요.')));
      return;
    }
    setState(() => _busy = true);
    try {
      final FormData form = FormData.fromMap({
        'btitle': _title.text.trim(),
        'bcontent': _content.text.trim(),
        if (_file != null) 'bfile': MultipartFile.fromBytes(await _file!.readAsBytes(), filename: _file!.name),
      });
      if (_isEdit) {
        final int id = asInt(widget.editing!['id']);
        await DioClient.instance.patch('/api/notices/$id', data: form);
        ref.invalidate(noticeDetailProvider(id));
      } else {
        await DioClient.instance.post('/api/notices', data: form);
      }
      ref.invalidate(noticePageProvider);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e, fallback: '저장하지 못했습니다.'))));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final String oldFile = widget.editing?['bfile']?.toString() ?? '';
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEdit ? '공지 수정' : '공지 작성'),
        actions: [TextButton(onPressed: _busy ? null : _submit, child: Text(_isEdit ? '수정' : '등록'))],
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        TextField(controller: _title, maxLength: 200, decoration: const InputDecoration(labelText: '제목 *', border: OutlineInputBorder())),
        const SizedBox(height: 8),
        TextField(
          controller: _content,
          minLines: 8,
          maxLines: 16,
          decoration: const InputDecoration(labelText: '내용 *', alignLabelWithHint: true, border: OutlineInputBorder()),
        ),
        const SizedBox(height: 14),
        OutlinedButton.icon(onPressed: _pick, icon: const Icon(Icons.attach_file), label: const Text('첨부 이미지 선택 (선택)')),
        const SizedBox(height: 10),
        if (_preview != null)
          ClipRRect(borderRadius: BorderRadius.circular(8), child: Image.memory(_preview!, height: 180, fit: BoxFit.cover))
        else if (oldFile.isNotEmpty)
          Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('현재 첨부 (새로 고르면 교체됩니다)', style: TextStyle(color: Colors.grey, fontSize: 12)),
            const SizedBox(height: 6),
            ClipRRect(borderRadius: BorderRadius.circular(8), child: NetImage(imageUrl(oldFile), height: 180, fallbackIcon: Icons.attach_file)),
          ]),
      ]),
    );
  }
}
