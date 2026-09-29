import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/dio_client.dart';
import '../../../shared/app_layout.dart';
import '../../home/data/home_provider.dart';

/// 매장 지도 - OpenStreetMap(flutter_map, 키 불필요) + 매장 마커 + 내 위치 + 가까운 순 목록
/// 매장을 누르면 카카오맵 길찾기(웹)로 연결
class StoreMapPage extends ConsumerStatefulWidget {
  const StoreMapPage({super.key});

  @override
  ConsumerState<StoreMapPage> createState() => _StoreMapPageState();
}

class _StoreMapPageState extends ConsumerState<StoreMapPage> {
  final MapController _map = MapController();
  LatLng? _me;

  @override
  void initState() {
    super.initState();
    currentPosition().then((Position? p) {
      if (p != null && mounted) setState(() => _me = LatLng(p.latitude, p.longitude));
    });
  }

  double _distanceKm(Map<String, dynamic> s) {
    if (_me == null) return 0;
    return Distance().as(LengthUnit.Meter, _me!, LatLng((s['lat'] as num).toDouble(), (s['lng'] as num).toDouble())) / 1000;
  }

  Future<void> _route(Map<String, dynamic> s) async {
    final Uri uri = Uri.parse('https://map.kakao.com/link/to/${Uri.encodeComponent(s['name'].toString())},${s['lat']},${s['lng']}');
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<List<Map<String, dynamic>>> stores = ref.watch(storesProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('매장 지도')),
      body: stores.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => EmptyView(message: errorMessage(e), onRetry: () => ref.invalidate(storesProvider)),
        data: (list) {
          final List<Map<String, dynamic>> sorted = [...list];
          if (_me != null) sorted.sort((a, b) => _distanceKm(a).compareTo(_distanceKm(b)));
          return Column(children: [
            Expanded(
              flex: 3,
              child: FlutterMap(
                mapController: _map,
                options: MapOptions(initialCenter: _me ?? const LatLng(37.5, 127.0), initialZoom: 7.5),
                children: [
                  TileLayer(
                    urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                    userAgentPackageName: 'com.thejoa703.bookstore',
                  ),
                  MarkerLayer(markers: [
                    for (final Map<String, dynamic> s in list)
                      Marker(
                        point: LatLng((s['lat'] as num).toDouble(), (s['lng'] as num).toDouble()),
                        width: 40,
                        height: 40,
                        child: GestureDetector(
                          onTap: () => _showStore(s),
                          child: const Icon(Icons.location_on, color: Colors.red, size: 38),
                        ),
                      ),
                    if (_me != null)
                      Marker(point: _me!, width: 24, height: 24, child: const Icon(Icons.my_location, color: Colors.blue)),
                  ]),
                  const RichAttributionWidget(attributions: [TextSourceAttribution('OpenStreetMap contributors')]),
                ],
              ),
            ),
            Expanded(
              flex: 2,
              child: ListView.separated(
                itemCount: sorted.length,
                separatorBuilder: (_, _) => const Divider(height: 1),
                itemBuilder: (_, i) {
                  final Map<String, dynamic> s = sorted[i];
                  return ListTile(
                    leading: const Icon(Icons.store_mall_directory_outlined),
                    title: Text(s['name'].toString()),
                    subtitle: Text('${s['address']}\n${s['hours']}${_me != null ? ' · ${_distanceKm(s).toStringAsFixed(1)}km' : ''}'),
                    isThreeLine: true,
                    trailing: IconButton(icon: const Icon(Icons.directions), onPressed: () => _route(s)),
                    onTap: () => _map.move(LatLng((s['lat'] as num).toDouble(), (s['lng'] as num).toDouble()), 15),
                  );
                },
              ),
            ),
          ]);
        },
      ),
    );
  }

  void _showStore(Map<String, dynamic> s) {
    showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(s['name'].toString(), style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            Text(s['address'].toString()),
            Text('영업시간 ${s['hours']} · ${s['phone']}', style: const TextStyle(color: Colors.grey)),
            const SizedBox(height: 12),
            FilledButton.icon(onPressed: () => _route(s), icon: const Icon(Icons.directions), label: const Text('카카오맵 길찾기')),
          ]),
        ),
      ),
    );
  }
}
