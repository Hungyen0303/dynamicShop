import 'package:flutter/foundation.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import 'storefront_repository.dart';

/// Điều phối 3 tầng fallback cho MỘT slug đang xem — dùng `ChangeNotifier` có sẵn của
/// Flutter (không thêm package quản lý state ngoài danh sách đã duyệt).
///
/// Quy tắc (docs/10-customer-app.md mục "Mạng yếu" + "Fallback ba tầng"):
/// - Mở app → render cache NGAY nếu có, không spinner toàn màn hình.
/// - Luôn fetch nền để lấy bản mới nhất.
/// - Cache trống VÀ network lỗi → bundled. Không bao giờ trắng màn hình.
class StorefrontController extends ChangeNotifier {
  StorefrontController({required this.repository, required String slug}) : _slug = slug;

  final StorefrontRepository repository;
  String _slug;
  String get slug => _slug;

  Map<String, dynamic>? layout;
  Map<String, dynamic>? data;
  StorefrontSource? source;
  bool isRefreshing = false;
  String? lastError;

  Future<void> bootstrap() async {
    final cached = await repository.loadCached(_slug);
    if (cached != null) {
      await _apply(cached);
    }
    await refresh();
  }

  Future<void> refresh() async {
    isRefreshing = true;
    if (layout == null) notifyListeners(); // chưa có gì để hiện — cho phép 1 lần loading tối thiểu
    try {
      final fresh = await repository.fetchFresh(_slug);
      lastError = null;
      await _apply(fresh);
    } catch (e) {
      lastError = e.toString();
      Telemetry.log('storefront_fetch_failed', {'slug': _slug, 'error': '$e'});
      if (layout == null) {
        // Cả tầng 1 (network) lẫn tầng 2 (cache, đã thử ở bootstrap) đều không có gì —
        // rơi xuống tầng 3, KHÔNG bao giờ để màn hình trắng.
        final bundled = await repository.loadBundled();
        await _apply(bundled);
      }
    } finally {
      isRefreshing = false;
      notifyListeners();
    }
  }

  Future<void> switchSlug(String newSlug) async {
    if (newSlug == _slug) return;
    _slug = newSlug;
    layout = null;
    data = null;
    source = null;
    notifyListeners();
    await bootstrap();
  }

  Future<void> _apply(StorefrontLoadResult result) async {
    // Preload font TRƯỚC khi hiện layout mới — tránh nháy đổi font (docs/10-customer-app.md
    // mục "Font"). Với 2 font bundled (Be Vietnam Pro, Inter) hàm này trả về ngay lập tức,
    // không có độ trễ mạng nào cho 2 mock shop demo.
    final theme = DsTokens.fromThemeJson(result.layout.obj('theme'));
    await preloadFont(theme.fontFamily);

    layout = result.layout;
    data = result.data;
    source = result.source;
    notifyListeners();
  }
}
