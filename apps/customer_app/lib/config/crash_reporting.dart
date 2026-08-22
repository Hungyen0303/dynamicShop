import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/foundation.dart';

/// Stage 1 (docs/70-stages.md) — bật Crashlytics khi có Firebase project thật. CHƯA có cho
/// flavor nào lúc viết (dev/staging/prod) — xem missing_config.md mục 6. `Firebase.initializeApp()`
/// không tìm được cấu hình thật sẽ ném lỗi ở runtime; BẮT LẠI ở đây để app vẫn mở bình thường,
/// không màn hình trắng (đúng tinh thần bất biến "không màn hình trắng" docs/10-customer-app.md,
/// áp dụng luôn cho lỗi khởi tạo hạ tầng ngoài SDUI).
class CrashReporting {
  static bool _enabled = false;

  static bool get isEnabled => _enabled;

  static Future<void> init() async {
    try {
      await Firebase.initializeApp();
      FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterFatalError;
      PlatformDispatcher.instance.onError = (Object error, StackTrace stack) {
        FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);
        return true;
      };
      _enabled = true;
    } catch (e) {
      // Chưa có Firebase project thật (missing_config.md mục 6) — log ra console thay vì
      // Crashlytics, KHÔNG crash app, KHÔNG throw tiếp.
      debugPrint('[crash-reporting] Firebase chưa cấu hình, bỏ qua: $e');
      _enabled = false;
    }
  }
}
