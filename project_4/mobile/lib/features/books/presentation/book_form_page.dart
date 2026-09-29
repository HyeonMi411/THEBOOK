import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../data/book_provider.dart';

/// 도서 등록 / 수정 (관리자) - 3차 /books/new, 도서 수정 모달과 같은 항목
/// 서버 BookRequestDto: title·author·publisher·publishDate·category 필수 / ranking·reviewCount·rating·description·pages·price 선택
class BookFormPage extends ConsumerStatefulWidget {
  final Map<String, dynamic>? editing; // null 이면 신규 등록
  const BookFormPage({super.key, this.editing});

  @override
  ConsumerState<BookFormPage> createState() => _BookFormPageState();
}

class _BookFormPageState extends ConsumerState<BookFormPage> {
  final _formKey = GlobalKey<FormState>();
  late final Map<String, TextEditingController> _c = {
    for (final String k in ['title', 'author', 'publisher', 'publishDate', 'category', 'price', 'pages', 'ranking', 'rating', 'reviewCount', 'description'])
      k: TextEditingController(text: widget.editing?[k]?.toString() ?? ''),
  };
  XFile? _cover;
  Uint8List? _coverPreview;
  bool _busy = false;

  bool get _isEdit => widget.editing != null;

  @override
  void dispose() {
    for (final TextEditingController c in _c.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _pickDate() async {
    final DateTime now = DateTime.now();
    final DateTime initial = DateTime.tryParse(_c['publishDate']!.text) ?? now;
    final DateTime? d = await showDatePicker(context: context, initialDate: initial, firstDate: DateTime(1900), lastDate: DateTime(now.year + 2));
    if (d != null) {
      _c['publishDate']!.text = '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    }
  }

  Future<void> _pickCover() async {
    final XFile? f = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 1200, imageQuality: 85);
    if (f == null) return;
    final Uint8List bytes = await f.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('표지는 5MB 이하 이미지만 올릴 수 있어요.')));
      return;
    }
    setState(() {
      _cover = f;
      _coverPreview = bytes;
    });
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      final Map<String, dynamic> saved = await BookApi.saveBook(
        id: _isEdit ? asInt(widget.editing!['id']) : null,
        fields: {for (final MapEntry<String, TextEditingController> e in _c.entries) e.key: e.value.text},
        cover: _cover,
      );
      ref.invalidate(bookListProvider);
      ref.invalidate(bookDetailProvider(asInt(saved['id'])));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_isEdit ? '도서를 수정했습니다.' : '도서를 등록했습니다. 재고는 상세 화면에서 입력해 주세요.')));
      Navigator.pop(context, true);
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e, fallback: '저장하지 못했습니다.'))));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _input(String key, String label, {bool required = false, TextInputType? type, String? hint, int lines = 1, String? Function(String)? extra}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextFormField(
        controller: _c[key],
        keyboardType: type,
        maxLines: lines,
        decoration: InputDecoration(labelText: required ? '$label *' : label, hintText: hint, border: const OutlineInputBorder()),
        validator: (v) {
          final String t = (v ?? '').trim();
          if (required && t.isEmpty) return '$label은(는) 필수입니다.';
          if (t.isNotEmpty && extra != null) return extra(t);
          return null;
        },
      ),
    );
  }

  String? _int(String t) => int.tryParse(t) == null || int.parse(t) < 0 ? '0 이상의 숫자만 입력해 주세요.' : null;

  @override
  Widget build(BuildContext context) {
    final String oldCover = widget.editing?['bookCover']?.toString() ?? '';
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEdit ? '도서 수정' : '새 도서 등록'),
        actions: [TextButton(onPressed: _busy ? null : _submit, child: Text(_isEdit ? '수정' : '등록'))],
      ),
      body: Form(
        key: _formKey,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            InkWell(
              onTap: _pickCover,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: _coverPreview != null
                    ? Image.memory(_coverPreview!, width: 96, height: 136, fit: BoxFit.cover)
                    : NetImage(imageUrl(oldCover), width: 96, height: 136, fallbackIcon: Icons.add_photo_alternate_outlined),
              ),
            ),
            const SizedBox(width: 14),
            const Expanded(
              child: Text('표지를 눌러 이미지를 선택하세요.\nJPG · PNG · GIF · WEBP / 5MB 이하\n(수정 시 선택하지 않으면 기존 표지 유지)',
                  style: TextStyle(color: Colors.grey, height: 1.6)),
            ),
          ]),
          const SizedBox(height: 16),
          _input('title', '도서 제목', required: true),
          _input('author', '저자', required: true),
          _input('publisher', '출판사', required: true),
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: TextFormField(
              controller: _c['publishDate'],
              readOnly: true,
              onTap: _pickDate,
              decoration: const InputDecoration(labelText: '출판일 *', hintText: '연도-월-일', border: OutlineInputBorder(), suffixIcon: Icon(Icons.calendar_today)),
              validator: (v) => (v ?? '').isEmpty ? '출판일은 필수입니다.' : null,
            ),
          ),
          _input('category', '카테고리', required: true, hint: '예: IT, 소설, 인문 ...'),
          Row(children: [
            Expanded(child: _input('price', '판매 가격(원)', type: TextInputType.number, extra: _int)),
            const SizedBox(width: 10),
            Expanded(child: _input('pages', '페이지 수', type: TextInputType.number, extra: _int)),
          ]),
          Row(children: [
            Expanded(child: _input('ranking', '랭킹', hint: '예: TOP1')),
            const SizedBox(width: 10),
            Expanded(
              child: _input('rating', '평점', type: const TextInputType.numberWithOptions(decimal: true),
                  extra: (t) => (double.tryParse(t) ?? -1) < 0 || double.parse(t) > 5 ? '0~5 사이로 입력해 주세요.' : null),
            ),
            const SizedBox(width: 10),
            Expanded(child: _input('reviewCount', '리뷰 수', type: TextInputType.number, extra: _int)),
          ]),
          _input('description', '도서설명', lines: 6, hint: '도서 상세설명을 입력하세요.'),
          const SizedBox(height: 8),
          FilledButton(
            onPressed: _busy ? null : _submit,
            style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
            child: Text(_isEdit ? '수정하기' : '등록하기'),
          ),
        ]),
      ),
    );
  }
}
