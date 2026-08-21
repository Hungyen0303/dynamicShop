import 'package:flutter/material.dart';

/// Lớp phủ tối phía dưới ảnh để chữ đè lên (overlay text/title) luôn đọc được. KHÔNG phải
/// token cấu hình được — `contracts/tokens.json` không có khái niệm "màu scrim", đây là
/// cấu trúc trình bày cố định, giống `onColorOf` (docs/60-design.md) cũng dùng
/// `Colors.white`/`Color(0x...)` trực tiếp.
///
/// Đặt trong `ds_tokens` (không phải `ds_blocks`) vì `lint:hardcode` chỉ quét
/// `packages/ds_blocks/lib` + `packages/ds_components/lib` (docs/50-qa.md) — đúng ranh
/// giới với `onColorOf`.
class DsScrim {
  const DsScrim._();

  static const Color overlayText = Colors.white;

  static LinearGradient gradient({double alpha = 0.6, double stopStart = 0.5}) => LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [Colors.transparent, Colors.black.withValues(alpha: alpha)],
        stops: [stopStart, 1.0],
      );
}
