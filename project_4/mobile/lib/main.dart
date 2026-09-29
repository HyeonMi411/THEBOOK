import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // ProviderScope: Riverpod 전역 상태를 앱 전체에 주입 (Redux 의 <Provider> 역할)
  runApp(const ProviderScope(child: App()));
}
