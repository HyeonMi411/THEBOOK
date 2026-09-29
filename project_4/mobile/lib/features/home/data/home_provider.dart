import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

import '../../../core/network/dio_client.dart';

/// 서울시청 (위치 권한이 없거나 실패하면 이 좌표로 날씨/매장 거리 계산)
const double defaultLat = 37.5665;
const double defaultLon = 126.9780;

/// 현재 위치 - 권한을 거부하거나 위치 서비스가 꺼져 있으면 null (앱은 서울 기준으로 동작)
///
/// - Android 는 Google Play 서비스(Fused) 대신 기본 LocationManager 를 사용
///   → "Google 위치 정확도를 켜세요" 팝업이 위치를 요청할 때마다 뜨던 문제 방지
/// - 한 번 얻은 위치는 10분간 재사용하고, 거부/실패하면 이번 실행 동안은 다시 묻지 않음
///   → 홈 새로고침·매장 지도에서 권한/팝업이 반복되지 않도록
Position? _cachedPosition;
DateTime? _cachedAt;
bool _locationUnavailable = false;

Future<Position?> currentPosition() async {
  if (_locationUnavailable) return null;
  if (_cachedPosition != null && _cachedAt != null && DateTime.now().difference(_cachedAt!) < const Duration(minutes: 10)) {
    return _cachedPosition;
  }
  try {
    if (!await Geolocator.isLocationServiceEnabled()) {
      _locationUnavailable = true;
      return null;
    }
    LocationPermission perm = await Geolocator.checkPermission();
    if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();
    if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) {
      _locationUnavailable = true;
      return null;
    }
    final LocationSettings settings = defaultTargetPlatform == TargetPlatform.android
        ? AndroidSettings(accuracy: LocationAccuracy.low, forceLocationManager: true, timeLimit: const Duration(seconds: 8))
        : const LocationSettings(accuracy: LocationAccuracy.low, timeLimit: Duration(seconds: 8));
    Position? pos;
    try {
      pos = await Geolocator.getCurrentPosition(locationSettings: settings);
    } catch (_) {
      // 시간 초과 등 → 마지막으로 알려진 위치라도 사용
      pos = await Geolocator.getLastKnownPosition(forceAndroidLocationManager: true);
    }
    if (pos == null) {
      _locationUnavailable = true;
      return null;
    }
    // 대한민국 밖(예: 에뮬레이터 기본 위치 = 미국 캘리포니아)이면 내 위치를 쓰지 않고 서울 기준으로 동작
    // (기상청 격자는 한반도만 지원 → 오류 문구 대신 서울 날씨, 매장 거리도 의미 없는 값이 되지 않도록)
    if (pos.latitude < 33 || pos.latitude > 38.7 || pos.longitude < 124.5 || pos.longitude > 132) {
      return null;
    }
    _cachedPosition = pos;
    _cachedAt = DateTime.now();
    return pos;
  } catch (_) {
    _locationUnavailable = true;
    return null;
  }
}

/// 기상청 초단기실황 (서버가 위경도 → 격자 변환)
final weatherProvider = FutureProvider.autoDispose<Map<String, dynamic>>((ref) async {
  final Position? pos = await currentPosition();
  final Response<dynamic> res = await DioClient.instance.get('/api/util/weather', queryParameters: {
    'lat': pos?.latitude ?? defaultLat,
    'lon': pos?.longitude ?? defaultLon,
  });
  return {...Map<String, dynamic>.from(res.data as Map), 'usedMyLocation': pos != null};
});

/// 도서 뉴스 (서버가 RSS 를 30분마다 크롤링해 둔 목록)
final bookNewsProvider = FutureProvider.autoDispose<List<Map<String, dynamic>>>((ref) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/util/book-news');
  return ((res.data as Map)['items'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
});

/// 공지사항 (페이지 단위) GET /api/notices?page=&size=
final noticePageProvider = FutureProvider.autoDispose.family<List<Map<String, dynamic>>, int>((ref, size) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/notices', queryParameters: {'page': 1, 'size': size});
  return ((res.data as Map)['content'] as List? ?? []).map((e) => Map<String, dynamic>.from(e as Map)).toList();
});

final noticeDetailProvider = FutureProvider.autoDispose.family<Map<String, dynamic>, int>((ref, id) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/notices/$id');
  return Map<String, dynamic>.from(res.data as Map);
});

/// 오프라인 매장 목록 (지도 마커)
final storesProvider = FutureProvider.autoDispose<List<Map<String, dynamic>>>((ref) async {
  final Response<dynamic> res = await DioClient.instance.get('/api/util/stores');
  return (res.data as List).map((e) => Map<String, dynamic>.from(e as Map)).toList();
});
