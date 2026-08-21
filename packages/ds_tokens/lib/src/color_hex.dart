import 'package:flutter/painting.dart';

/// Parse `#RRGGBB` (đúng format `contracts/storefront.schema.json` quy định cho theme
/// color). Khoan dung: sai format trả `null` thay vì throw — caller tự rơi về default.
Color? colorFromHex(String? hex) {
  if (hex == null) return null;
  final match = RegExp(r'^#([0-9A-Fa-f]{6})$').firstMatch(hex);
  if (match == null) return null;
  final value = int.tryParse(match.group(1)!, radix: 16);
  if (value == null) return null;
  return Color(0xFF000000 | value);
}
