import 'package:flutter/material.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// `giá trị = blockOverride ?? variantPreset ?? tenantTheme ?? appDefault` — áp cho MỌI
/// thuộc tính, docs/60-design.md mục "Chuỗi phân giải". Không widget nào tự quyết thứ tự
/// riêng: mọi block dùng đúng một `StyleResolver` này.
class StyleResolver {
  const StyleResolver(this.overrides);

  /// `node.overrides` — chỉ những key nằm trong whitelist `overridable` của block đó
  /// trong `contracts/blocks.registry.json` (backend chịu trách nhiệm chỉ lưu đúng
  /// whitelist; client chỉ cần đọc, không cần validate lại whitelist ở đây).
  final Map<String, dynamic> overrides;

  /// [variant] là lớp preset (ví dụ `ProductCardStyle.radius`) — `null` nếu block không
  /// có khái niệm variant (hero_banner, promo_strip…).
  double radius(BuildContext context, {double? variant}) {
    return overrides.dbl('radius') ?? variant ?? context.tokens.radiusMd;
  }

  /// `bg` override là TÊN token (`"primary"`, `"surface"`…), không phải mã màu tự do —
  /// đúng tinh thần "chỉ token và whitelist" (docs/10-customer-app.md mục "Không làm").
  Color background(BuildContext context, {Color? variant}) {
    final tokenName = overrides.str('bg');
    final fromToken = tokenName == null ? null : _colorToken(context.tokens, tokenName);
    return fromToken ?? variant ?? context.tokens.surface;
  }

  double? overrideDouble(String key) => overrides.dbl(key);
  String? overrideString(String key) => overrides.str(key);
  bool? overrideBool(String key) => overrides[key] is bool ? overrides[key] as bool : null;

  /// Public — dùng khi một block cần resolve tên token thành `Color` ở NGOÀI `bg`
  /// override (ví dụ `promo_strip.bg_token`, giá trị cấu hình gốc trước khi có override).
  static Color? colorToken(DsTokens t, String? name) => name == null ? null : _colorToken(t, name);

  static Color? _colorToken(DsTokens t, String name) => switch (name) {
        'primary' => t.primary,
        'surface' => t.surface,
        'border' => t.border,
        'muted' => t.muted,
        'danger' => t.danger,
        'success' => t.success,
        'warning' => t.warning,
        _ => null,
      };
}
