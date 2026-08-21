import 'package:flutter/material.dart';

import 'app.dart';
import 'config/app_config.dart';

/// `flutter run --flavor prod -t lib/main_prod.dart`
///
/// Chưa có server production thật ở Stage 0 — URL dưới đây là placeholder, cần xác nhận
/// domain thật trước khi build release.
void main() {
  AppConfig.instance = const AppConfig(
    flavor: 'prod',
    apiBaseUrl: 'https://api.dynamicshop.vn',
    defaultTenantSlug: 'bun-co-ba',
  );
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const CustomerApp());
}
