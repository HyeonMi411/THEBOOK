import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../data/board_provider.dart';

/// 글쓰기 / 수정 - 이미지 최대 5장, 해시태그는 "#책 #추천" 처럼 공백·쉼표로 구분 (서버와 같은 규칙)
class PostWritePage extends ConsumerStatefulWidget {
  final Map<String, dynamic>? editing;
  const PostWritePage({super.key, this.editing});

  @override
  ConsumerState<PostWritePage> createState() => _PostWritePageState();
}

class _PostWritePageState extends ConsumerState<PostWritePage> {
  final TextEditingController _content = TextEditingController();
  final TextEditingController _tags = TextEditingController();
  final List<XFile> _images = [];
  final List<Uint8List> _previews = [];
  bool _busy = false;

  bool get _isEdit => widget.editing != null;

  @override
  void initState() {
    super.initState();
    if (_isEdit) {
      _content.text = widget.editing!['content']?.toString() ?? '';
      _tags.text = ((widget.editing!['hashtags'] as List?) ?? []).map((t) => '#$t').join(' ');
    }
  }

  @override
  void dispose() {
    _content.dispose();
    _tags.dispose();
    super.dispose();
  }

  Future<void> _pick() async {
    final List<XFile> picked = await ImagePicker().pickMultiImage(maxWidth: 1600, imageQuality: 85);
    if (picked.isEmpty) return;
    final List<XFile> take = picked.take(5 - _images.length).toList();
    for (final XFile f in take) {
      final Uint8List bytes = await f.readAsBytes();
      if (bytes.length > 5 * 1024 * 1024) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${f.name}: 5MB 이하 사진만 올릴 수 있어요.')));
        continue;
      }
      _images.add(f);
      _previews.add(bytes);
    }
    setState(() {});
  }

  Future<void> _submit() async {
    final String content = _content.text.trim();
    if (content.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('내용을 입력해 주세요.')));
      return;
    }
    setState(() => _busy = true);
    try {
      if (_isEdit) {
        await BoardApi.update(asInt(widget.editing!['id']), content, _tags.text, _images);
      } else {
        await BoardApi.create(content, _tags.text, _images);
      }
      ref.invalidate(feedProvider);
      ref.invalidate(hashtagsProvider);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final List<dynamic> oldImages = (widget.editing?['imageUrls'] as List?) ?? [];
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEdit ? '글 수정' : '글쓰기'),
        actions: [TextButton(onPressed: _busy ? null : _submit, child: Text(_isEdit ? '수정' : '등록'))],
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        TextField(
          controller: _content,
          maxLength: 4000,
          minLines: 6,
          maxLines: 12,
          decoration: const InputDecoration(hintText: '읽고 있는 책, 추천하고 싶은 문장을 나눠 보세요.', border: OutlineInputBorder()),
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _tags,
          decoration: const InputDecoration(
            labelText: '해시태그 (최대 10개)', hintText: '#소설 #추천', border: OutlineInputBorder(), prefixIcon: Icon(Icons.tag)),
        ),
        const SizedBox(height: 16),
        Row(children: [
          OutlinedButton.icon(
            onPressed: _images.length >= 5 ? null : _pick,
            icon: const Icon(Icons.add_photo_alternate_outlined),
            label: Text('사진 ${_images.length}/5'),
          ),
          if (_isEdit && oldImages.isNotEmpty && _images.isEmpty)
            const Padding(padding: EdgeInsets.only(left: 8), child: Text('새 사진을 고르면 기존 사진이 교체돼요.', style: TextStyle(fontSize: 12, color: Colors.grey))),
        ]),
        const SizedBox(height: 10),
        Wrap(spacing: 8, runSpacing: 8, children: [
          for (int i = 0; i < _previews.length; i++)
            Stack(children: [
              ClipRRect(borderRadius: BorderRadius.circular(6), child: Image.memory(_previews[i], width: 96, height: 96, fit: BoxFit.cover)),
              Positioned(
                right: 0, top: 0,
                child: InkWell(
                  onTap: () => setState(() {
                    _images.removeAt(i);
                    _previews.removeAt(i);
                  }),
                  child: const CircleAvatar(radius: 11, backgroundColor: Colors.black54, child: Icon(Icons.close, size: 14, color: Colors.white)),
                ),
              ),
            ]),
          if (_images.isEmpty)
            for (final dynamic u in oldImages)
              ClipRRect(borderRadius: BorderRadius.circular(6), child: NetImage(imageUrl(u), width: 96, height: 96, fallbackIcon: Icons.image)),
        ]),
      ]),
    );
  }
}
