import 'package:flutter/painting.dart';

import 'color_hex.dart';

/// Giá trị `default` sao y từ `contracts/tokens.json`. KHÔNG sửa ở đây khi muốn đổi màu
/// mặc định của app — sửa `default` trong `tokens.json` trước (xem docs/60-design.md mục
/// "Đổi màu mặc định"), rồi mới đồng bộ lại đây (Stage 0 chưa có generator).
class DsDefaults {
  const DsDefaults._();

  static final Color primary = colorFromHex('#E23744')!;
  static final Color surface = colorFromHex('#FFFFFF')!;
  static final Color border = colorFromHex('#E5E5E5')!;
  static final Color muted = colorFromHex('#757575')!;
  static final Color danger = colorFromHex('#D32F2F')!;
  static final Color success = colorFromHex('#2E7D32')!;
  static final Color warning = colorFromHex('#ED6C02')!;

  static const double radiusSm = 6;
  static const double radiusMd = 12;
  static const double radiusLg = 20;

  static const double spacingUnit = 8;

  static const String fontFamily = 'Be Vietnam Pro';

  static const List<String> allowedFonts = [
    'Be Vietnam Pro',
    'Inter',
    'Roboto',
    'Nunito Sans',
    'Lexend',
    'Open Sans',
    'Montserrat',
    'Quicksand',
  ];

  static const List<String> bundledFonts = ['Be Vietnam Pro', 'Inter'];
}
