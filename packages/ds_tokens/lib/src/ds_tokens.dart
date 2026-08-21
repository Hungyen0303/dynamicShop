import 'package:flutter/material.dart';
import 'package:ds_core/ds_core.dart';

import 'color_hex.dart';
import 'ds_defaults.dart';
import 'ds_typography_scale.dart';

/// `ThemeExtension<DsTokens>` — docs/60-design.md bất biến #2: component đọc token từ
/// `context` (`Theme.of(ctx).extension<DsTokens>()`), không nhận màu qua constructor.
/// KHÔNG tự viết `InheritedWidget` — dùng cơ chế theme extension có sẵn của Flutter để
/// animation đổi theme (đổi shop) chạy qua `lerp()` mượt mà.
@immutable
class DsTokens extends ThemeExtension<DsTokens> {
  const DsTokens({
    required this.primary,
    required this.surface,
    required this.border,
    required this.muted,
    required this.danger,
    required this.success,
    required this.warning,
    required this.radiusSm,
    required this.radiusMd,
    required this.radiusLg,
    required this.spacingUnit,
    required this.fontFamily,
    required this.scale,
  });

  /// Mặc định app — dùng khi chưa có theme tenant nào (bundled fallback, splash…).
  factory DsTokens.appDefault() => DsTokens(
        primary: DsDefaults.primary,
        surface: DsDefaults.surface,
        border: DsDefaults.border,
        muted: DsDefaults.muted,
        danger: DsDefaults.danger,
        success: DsDefaults.success,
        warning: DsDefaults.warning,
        radiusSm: DsDefaults.radiusSm,
        radiusMd: DsDefaults.radiusMd,
        radiusLg: DsDefaults.radiusLg,
        spacingUnit: DsDefaults.spacingUnit,
        fontFamily: DsDefaults.fontFamily,
        scale: DsTypographyScale.compact,
      );

  /// Parse `StorefrontResponse.layout.theme` — chỉ 4 field `configurable: true` /
  /// server thật sự gửi (`primary`, `surface`, `radius_md`, `font_family`, xem
  /// `contracts/storefront.schema.json`). Mọi field khác (`border`, `on_surface`,
  /// `danger`…) luôn dùng app default vì `configurable: false` — shop không được đổi.
  ///
  /// Khoan dung tuyệt đối: thiếu field, sai kiểu, hex sai format → rơi về default, không
  /// bao giờ throw (docs/10-customer-app.md mục "Parse JSON — luôn khoan dung").
  factory DsTokens.fromThemeJson(Map<String, dynamic> theme) {
    final fallback = DsTokens.appDefault();
    final font = theme.str('font_family');
    return DsTokens(
      primary: colorFromHex(theme.str('primary')) ?? fallback.primary,
      surface: colorFromHex(theme.str('surface')) ?? fallback.surface,
      border: fallback.border,
      muted: fallback.muted,
      danger: fallback.danger,
      success: fallback.success,
      warning: fallback.warning,
      radiusSm: fallback.radiusSm,
      radiusMd: theme.dbl('radius_md') ?? fallback.radiusMd,
      radiusLg: fallback.radiusLg,
      spacingUnit: fallback.spacingUnit,
      // font tự do không được nhận — chỉ chấp nhận nếu nằm trong allowed_fonts
      fontFamily: (font != null && DsDefaults.allowedFonts.contains(font)) ? font : fallback.fontFamily,
      scale: DsTypographyScale.compact,
    );
  }

  final Color primary;
  final Color surface;
  final Color border;
  final Color muted;
  final Color danger;
  final Color success;
  final Color warning;

  final double radiusSm;
  final double radiusMd;
  final double radiusLg;

  final double spacingUnit;

  final String fontFamily;
  final DsTypographyScale scale;

  /// Màu chữ tự suy từ luminance nền — docs/60-design.md "Màu chữ tự suy". Không bao giờ
  /// cho cấu hình riêng, ngăn shop tạo ra chữ trắng trên nền vàng nhạt.
  Color onColorOf(Color bg) => bg.computeLuminance() > 0.5 ? const Color(0xFF1A1A1A) : Colors.white;

  Color get onSurface => onColorOf(surface);
  Color get onPrimary => onColorOf(primary);

  /// Mọi spacing là bội số của `spacingUnit` (8) — docs/60-design.md "Spacing luôn là bội
  /// số của unit". Dùng `t.space(2)` thay vì `EdgeInsets.all(16)`.
  double space(num units) => spacingUnit * units;

  double get textScaleFactor => scale.textScaleFactor;

  @override
  DsTokens copyWith({
    Color? primary,
    Color? surface,
    Color? border,
    Color? muted,
    Color? danger,
    Color? success,
    Color? warning,
    double? radiusSm,
    double? radiusMd,
    double? radiusLg,
    double? spacingUnit,
    String? fontFamily,
    DsTypographyScale? scale,
  }) {
    return DsTokens(
      primary: primary ?? this.primary,
      surface: surface ?? this.surface,
      border: border ?? this.border,
      muted: muted ?? this.muted,
      danger: danger ?? this.danger,
      success: success ?? this.success,
      warning: warning ?? this.warning,
      radiusSm: radiusSm ?? this.radiusSm,
      radiusMd: radiusMd ?? this.radiusMd,
      radiusLg: radiusLg ?? this.radiusLg,
      spacingUnit: spacingUnit ?? this.spacingUnit,
      fontFamily: fontFamily ?? this.fontFamily,
      scale: scale ?? this.scale,
    );
  }

  @override
  DsTokens lerp(ThemeExtension<DsTokens>? other, double t) {
    if (other is! DsTokens) return this;
    return DsTokens(
      primary: Color.lerp(primary, other.primary, t) ?? other.primary,
      surface: Color.lerp(surface, other.surface, t) ?? other.surface,
      border: Color.lerp(border, other.border, t) ?? other.border,
      muted: Color.lerp(muted, other.muted, t) ?? other.muted,
      danger: Color.lerp(danger, other.danger, t) ?? other.danger,
      success: Color.lerp(success, other.success, t) ?? other.success,
      warning: Color.lerp(warning, other.warning, t) ?? other.warning,
      radiusSm: lerpDouble(radiusSm, other.radiusSm, t),
      radiusMd: lerpDouble(radiusMd, other.radiusMd, t),
      radiusLg: lerpDouble(radiusLg, other.radiusLg, t),
      spacingUnit: lerpDouble(spacingUnit, other.spacingUnit, t),
      // font/scale không nội suy được — đổi ngay ở nửa chặng animation
      fontFamily: t < 0.5 ? fontFamily : other.fontFamily,
      scale: t < 0.5 ? scale : other.scale,
    );
  }

  static double lerpDouble(double a, double b, double t) => a + (b - a) * t;
}
