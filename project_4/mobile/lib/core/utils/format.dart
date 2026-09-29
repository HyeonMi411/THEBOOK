import '../network/api_client.dart';

/// 12345 → "12,345원"
String won(dynamic value) {
  final int n = value is num ? value.toInt() : int.tryParse('$value') ?? 0;
  final String s = n.abs().toString();
  final StringBuffer buf = StringBuffer();
  for (int i = 0; i < s.length; i++) {
    if (i > 0 && (s.length - i) % 3 == 0) buf.write(',');
    buf.write(s[i]);
  }
  return '${n < 0 ? '-' : ''}${buf.toString()}원';
}

/// "2026-09-29T14:30:12" → "2026-09-29 14:30"
String shortDateTime(dynamic value) {
  final String s = value?.toString() ?? '';
  if (s.length >= 16 && s.contains('T')) {
    return s.substring(0, 16).replaceFirst('T', ' ');
  }
  return s;
}

/// "2026-09-29T..." → "2026-09-29"
String shortDate(dynamic value) {
  final String s = value?.toString() ?? '';
  return s.length >= 10 ? s.substring(0, 10) : s;
}

/// 서버 이미지 경로("uploads/a.png", "/uploads/a.png") 또는 외부 URL → 화면에서 쓸 전체 URL
String imageUrl(dynamic path) {
  final String url = path?.toString() ?? '';
  if (url.isEmpty) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  final String base = ApiClient.getBaseUrl();
  return url.startsWith('/') ? '$base$url' : '$base/$url';
}

int asInt(dynamic v, [int fallback = 0]) => v is num ? v.toInt() : int.tryParse('$v') ?? fallback;
