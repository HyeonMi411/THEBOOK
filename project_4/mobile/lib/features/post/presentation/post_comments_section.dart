import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/utils/format.dart';
import '../../../shared/app_layout.dart';
import '../../auth/data/auth_provider.dart';
import '../data/board_provider.dart';

class PostCommentsSection extends ConsumerStatefulWidget {
  final int postId;
  final VoidCallback? onChanged;
  const PostCommentsSection({super.key, required this.postId, this.onChanged});

  @override
  ConsumerState<PostCommentsSection> createState() => _PostCommentsSectionState();
}

class _PostCommentsSectionState extends ConsumerState<PostCommentsSection> {
  final TextEditingController _input = TextEditingController();
  bool _sending = false;

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final String text = _input.text.trim();
    if (text.isEmpty || !requireLogin(context, ref)) return;
    setState(() => _sending = true);
    try {
      await BoardApi.addComment(widget.postId, text);
      _input.clear();
      ref.invalidate(commentsProvider(widget.postId));
      widget.onChanged?.call();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _delete(int commentId) async {
    try {
      await BoardApi.deleteComment(widget.postId, commentId);
      ref.invalidate(commentsProvider(widget.postId));
      widget.onChanged?.call();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<List<Map<String, dynamic>>> comments = ref.watch(commentsProvider(widget.postId));
    final int? myId = ref.watch(authProvider).userId;
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text('댓글 ${comments.maybeWhen(data: (c) => c.length, orElse: () => 0)}', style: const TextStyle(fontWeight: FontWeight.bold)),
      const SizedBox(height: 8),
      Row(children: [
        Expanded(
          child: TextField(
            controller: _input,
            maxLength: 1000,
            minLines: 1,
            maxLines: 3,
            decoration: const InputDecoration(hintText: '댓글을 입력하세요', border: OutlineInputBorder(), isDense: true, counterText: ''),
          ),
        ),
        IconButton(onPressed: _sending ? null : _send, icon: const Icon(Icons.send)),
      ]),
      comments.when(
        loading: () => const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
        error: (e, _) => Padding(padding: const EdgeInsets.all(8), child: Text(errorMessage(e))),
        data: (list) => Column(children: [
          for (final Map<String, dynamic> c in list)
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(c['userNickname']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
              subtitle: Text('${c['content']}\n${shortDateTime(c['createdAt'])}'),
              isThreeLine: true,
              trailing: myId != null && myId == asInt(c['userId'])
                  ? IconButton(icon: const Icon(Icons.delete_outline, size: 20), onPressed: () => _delete(asInt(c['id'])))
                  : null,
            ),
        ]),
      ),
    ]);
  }
}
