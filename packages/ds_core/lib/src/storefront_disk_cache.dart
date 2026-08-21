import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// Tầng 2 của fallback ba tầng (docs/10-customer-app.md mục "Fallback ba tầng"):
/// server (fresh) → cache đĩa (đây) → `assets/default_storefront.json` (bundled).
///
/// Key theo `tenant_id + schema_version`. Customer_app ở public plane không có
/// `tenant_id` dạng UUID (khách không đăng nhập) — dùng **slug** làm khoá thay thế, vì
/// slug xác định đúng một tenant trong toàn bộ vòng đời app. Ghi rõ ở đây vì đây là một
/// quyết định thay thế so với chữ dùng trong doc ("tenant_id"), không phải sai lệch.
///
/// Không bao giờ ném lỗi ra ngoài — nếu đĩa lỗi (đầy, quyền, …) thì coi như cache miss,
/// để caller rơi xuống tầng 3 (bundled) thay vì crash.
///
/// Có timeout phòng thủ cho MỌI thao tác — máy Android tầm thấp có thể có bộ nhớ trong
/// chậm bất thường (thẻ nhớ rẻ, đầy dung lượng…). Tầng 2 của fallback ba tầng KHÔNG được
/// phép làm app đứng hình lâu hơn network timeout của tầng 1 — nếu không, "không bao giờ
/// màn hình trắng" biến thành "không bao giờ hiện màn hình".
///
/// `directoryProvider` cho phép thay `getApplicationSupportDirectory()` bằng thư mục
/// khác lúc test — gọi channel plugin thật trong `flutter test` (Dart VM host, không có
/// engine chạy) có thể TREO VÔ THỜI HẠN thay vì ném lỗi nhanh như mong đợi (một số nền
/// tảng desktop implement bằng lời gọi hệ thống đồng bộ, chặn cả isolate nên
/// `Future.timeout` cũng không cứu được) — đây là quan sát thật khi viết test package
/// này, không phải giả định. Trên Android thật, `path_provider` luôn là platform channel
/// bất đồng bộ nên không gặp vấn đề này.
class StorefrontDiskCache {
  const StorefrontDiskCache({this.timeout = const Duration(seconds: 3), Future<Directory> Function()? directoryProvider})
      : _directoryProvider = directoryProvider ?? getApplicationSupportDirectory;

  final Duration timeout;
  final Future<Directory> Function() _directoryProvider;

  Future<void> save(String slug, int schemaVersion, String rawJson) async {
    try {
      await _run(() async {
        final file = await _fileFor(slug, schemaVersion);
        await file.writeAsString(rawJson, flush: true);
      });
    } catch (_) {
      // cache là best-effort — lỗi/timeout ghi đĩa không được làm hỏng flow chính
    }
  }

  Future<Map<String, dynamic>?> load(String slug, int schemaVersion) async {
    try {
      return await _run(() async {
        final file = await _fileFor(slug, schemaVersion);
        if (!await file.exists()) return null;
        final raw = await file.readAsString();
        final decoded = jsonDecode(raw);
        return decoded is Map<String, dynamic> ? decoded : null;
      });
    } catch (_) {
      return null;
    }
  }

  Future<T> _run<T>(Future<T> Function() action) => Future(action).timeout(timeout);

  Future<File> _fileFor(String slug, int schemaVersion) async {
    final dir = await _directoryProvider();
    final cacheDir = Directory('${dir.path}/storefront_cache');
    if (!await cacheDir.exists()) {
      await cacheDir.create(recursive: true);
    }
    final safeSlug = slug.replaceAll(RegExp(r'[^a-zA-Z0-9_-]'), '_');
    return File('${cacheDir.path}/${safeSlug}_v$schemaVersion.json');
  }
}
