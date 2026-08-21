import 'package:ds_core/ds_core.dart';

import '../config/app_config.dart';

/// Shop khách đang xem — Stage 0 chỉ dùng để màn hình dev đổi giữa 2 mock shop
/// (bun-co-ba / tra-sua-ngoc). Ở bản thật (một shop = một flavor, xem
/// docs/10-customer-app.md mục "Flavor"), giá trị này luôn cố định theo
/// `AppConfig.defaultTenantSlug`, không đổi được từ UI.
class TenantSession {
  TenantSession({KvStore? kvStore}) : _kv = kvStore ?? const KvStore();

  static const _key = 'current_tenant_slug';

  final KvStore _kv;

  Future<String> currentSlug() async {
    final saved = await _kv.getString(_key);
    return saved ?? AppConfig.instance.defaultTenantSlug;
  }

  Future<void> setSlug(String slug) => _kv.setString(_key, slug);
}
