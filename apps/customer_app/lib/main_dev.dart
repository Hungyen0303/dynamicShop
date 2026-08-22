import 'package:flutter/material.dart';

import 'app.dart';
import 'config/app_config.dart';
import 'config/crash_reporting.dart';

/// `flutter run --flavor dev -t lib/main_dev.dart`
///
/// 🔴 Bẫy mạng: máy THẬT qua adb (không phải emulator) không thấy `10.0.2.2`. Chạy
/// `adb reverse tcp:8080 tcp:8080` trước, backend local coi như đang chạy NGAY trên máy
/// thật ở `localhost:8080`. `10.0.2.2` chỉ đúng cho Android emulator.
void main() async {
  AppConfig.instance = const AppConfig(
    flavor: 'dev',
    apiBaseUrl: 'http://localhost:8080',
    defaultTenantSlug: 'bun-co-ba',
  );
  WidgetsFlutterBinding.ensureInitialized();
  await CrashReporting.init();
  runApp(const CustomerApp());
}
