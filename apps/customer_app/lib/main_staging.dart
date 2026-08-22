import 'package:flutter/material.dart';

import 'app.dart';
import 'config/app_config.dart';
import 'config/crash_reporting.dart';

/// `flutter run --flavor staging -t lib/main_staging.dart`
///
/// Chưa có server staging thật ở Stage 0 (dựng ở Stage 1, xem docs/70-stages.md) — URL
/// dưới đây là placeholder, cần xác nhận domain thật trước khi dùng.
void main() async {
  AppConfig.instance = const AppConfig(
    flavor: 'staging',
    apiBaseUrl: 'https://staging-api.dynamicshop.vn',
    defaultTenantSlug: 'bun-co-ba',
  );
  WidgetsFlutterBinding.ensureInitialized();
  await CrashReporting.init();
  runApp(const CustomerApp());
}
