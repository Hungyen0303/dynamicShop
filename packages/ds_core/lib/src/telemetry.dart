import 'package:flutter/foundation.dart';

/// Hai chốt chặn bắt buộc của SDUI registry (docs/10-customer-app.md mục "Registry — hai
/// chốt chặn bắt buộc") đều gọi qua đây. Stage 0 chỉ log ra console (`debugPrint`) — chưa
/// có Crashlytics (đó là Stage 1). KHÔNG được xoá lời gọi Telemetry ở registry dù chưa có
/// backend thật nhận log, vì đó chính là chỗ tương lai gắn Crashlytics vào.
class Telemetry {
  const Telemetry._();

  /// (1) forward compat — block type không có trong registry, ví dụ app cũ chưa cập nhật
  /// nhưng shop đã dùng block mới.
  static void unknownBlock(String type, int version) {
    debugPrint('[telemetry] unknown_block type=$type v=$version');
  }

  /// (2) error boundary — build một block cụ thể ném lỗi, không được để trắng cả màn hình.
  static void blockRenderError(String blockId, String type, Object error, StackTrace stack) {
    debugPrint('[telemetry] block_render_error id=$blockId type=$type error=$error');
  }

  static void log(String event, [Map<String, Object?>? data]) {
    debugPrint('[telemetry] $event ${data ?? ''}');
  }
}
