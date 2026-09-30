import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/dio_client.dart';
import '../../../core/network/upload.dart';

List<Map<String, dynamic>> _toList(dynamic data) =>
    (data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();

/// 커뮤니티 피드 조건
class FeedQuery {
  final String feed; // all | following
  final String? tag;
  const FeedQuery({this.feed = 'all', this.tag});

  @override
  bool operator ==(Object other) => other is FeedQuery && other.feed == feed && other.tag == tag;

  @override
  int get hashCode => Object.hash(feed, tag);
}

/// GET /api/posts?feed=&tag=
final feedProvider = FutureProvider.autoDispose.family<List<Map<String, dynamic>>, FeedQuery>((ref, q) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/posts', queryParameters: {
    if (q.feed == 'following') 'feed': 'following',
    if (q.tag != null && q.tag!.isNotEmpty) 'tag': q.tag,
  });
  return _toList(res.data);
});

final hashtagsProvider = FutureProvider.autoDispose<List<String>>((ref) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/posts/hashtags');
  return (res.data as List).map((e) => e.toString()).toList();
});

final postDetailProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, int>((ref, id) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/posts/$id');
  return Map<String, dynamic>.from(res.data as Map);
});

final userProfileProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, int>((ref, id) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/users/$id/profile');
  return Map<String, dynamic>.from(res.data as Map);
});

final userPostsProvider = FutureProvider.autoDispose.family<List<Map<String, dynamic>>, int>((ref, id) async {
  return _toList((await DioClient.instance.get('/api/users/$id/posts')).data);
});

final likedPostsProvider = FutureProvider.autoDispose<List<Map<String, dynamic>>>((ref) async {
  return _toList((await DioClient.instance.get('/api/users/me/liked-posts')).data);
});

/// (userId, followers|followings)
final followListProvider =
    FutureProvider.autoDispose.family<List<Map<String, dynamic>>, (int, String)>((ref, arg) async {
  return _toList((await DioClient.instance.get('/api/users/${arg.$1}/${arg.$2}')).data);
});

final commentsProvider = FutureProvider.autoDispose.family<List<Map<String, dynamic>>, int>((ref, postId) async {
  return _toList((await DioClient.instance.get('/api/posts/$postId/comments')).data);
});

/// 쓰기 동작 모음 (결과만 돌려주고, 화면이 필요한 Provider 를 invalidate)
class BoardApi {
  static Dio get _dio => DioClient.instance;

  /// → {liked, likeCount}
  static Future<Map<String, dynamic>> toggleLike(int postId) async =>
      Map<String, dynamic>.from((await _dio.post('/api/posts/$postId/like')).data as Map);

  /// → {retweeted, retweetCount}
  static Future<Map<String, dynamic>> toggleRetweet(int postId) async =>
      Map<String, dynamic>.from((await _dio.post('/api/posts/$postId/retweet')).data as Map);

  /// → {following, followerCount}
  static Future<Map<String, dynamic>> toggleFollow(int userId) async =>
      Map<String, dynamic>.from((await _dio.post('/api/users/$userId/follow')).data as Map);

  static Future<FormData> _form(String content, String hashtags, List<XFile> images) async {
    final List<MultipartFile> files = [];
    for (final XFile f in images) {
      files.add(await imagePart(f));
    }
    return FormData.fromMap({'content': content, 'hashtags': hashtags, if (files.isNotEmpty) 'files': files});
  }

  static Future<void> create(String content, String hashtags, List<XFile> images) async =>
      _dio.post('/api/posts', data: await _form(content, hashtags, images));

  /// images 가 비어 있으면 기존 이미지 유지, 있으면 교체 (서버 정책)
  static Future<void> update(int postId, String content, String hashtags, List<XFile> images) async =>
      _dio.patch('/api/posts/$postId', data: await _form(content, hashtags, images));

  static Future<void> delete(int postId) => _dio.delete('/api/posts/$postId');

  static Future<void> addComment(int postId, String content) =>
      _dio.post('/api/posts/$postId/comments', data: {'content': content});

  static Future<void> deleteComment(int postId, int commentId) => _dio.delete('/api/posts/$postId/comments/$commentId');
}
