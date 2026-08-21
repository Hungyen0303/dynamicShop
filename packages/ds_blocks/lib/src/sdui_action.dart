import 'package:ds_core/ds_core.dart';

/// `contracts/blocks.registry.json#/action_types` — `hero_banner`/`image_banner` có prop
/// `action` tuỳ chọn. Chỉ 3 loại, không biểu thức tự do (bất biến SDUI mức 2).
enum SduiActionType { openCategory, openProduct, externalUrl }

class SduiAction {
  const SduiAction({required this.type, this.categoryId, this.productId, this.url});

  /// Trả `null` nếu prop rỗng hoặc `type` không khớp — caller coi như "không có action",
  /// không throw (khoan dung — xem docs/10-customer-app.md mục "Parse JSON").
  static SduiAction? fromProp(dynamic raw) {
    if (raw is! Map) return null;
    final json = Map<String, dynamic>.from(raw);
    final type = json.str('type');
    return switch (type) {
      'open_category' => SduiAction(type: SduiActionType.openCategory, categoryId: json.str('id')),
      'open_product' => SduiAction(type: SduiActionType.openProduct, productId: json.str('id')),
      'external_url' => SduiAction(type: SduiActionType.externalUrl, url: json.str('url')),
      _ => null,
    };
  }

  final SduiActionType type;
  final String? categoryId;
  final String? productId;
  final String? url;
}
