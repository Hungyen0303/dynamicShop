import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'ds_defaults.dart';
import 'ds_tokens.dart';

/// Cách DUY NHẤT ds_components/ds_blocks được tạo `TextStyle` có font tenant — không bao
/// giờ `TextStyle(fontFamily: t.fontFamily)` trực tiếp. Lý do (docs/60-design.md mục
/// "Font — cái bẫy tiếng Việt" + docs/10-customer-app.md mục "Font"):
///
/// - Font trong `bundled_fonts` (Be Vietnam Pro, Inter) đã đăng ký thẳng qua
///   `pubspec.yaml` của app — dùng tên family thường là đủ, không cần qua GoogleFonts,
///   hiện ngay không cần chờ mạng.
/// - Font còn lại trong `allowed_fonts` PHẢI đi qua `GoogleFonts.getFont()` — package
///   `google_fonts` đăng ký font đã tải dưới một tên family GHÉP (không phải tên gốc như
///   "Quicksand"), nên `TextStyle(fontFamily: 'Quicksand')` viết tay sẽ KHÔNG khớp font
///   đã tải và âm thầm rơi về font hệ thống.
extension DsTextStyleX on DsTokens {
  TextStyle textStyle({double? fontSize, FontWeight? fontWeight, Color? color, double? height}) {
    final base = TextStyle(fontSize: fontSize, fontWeight: fontWeight, color: color, height: height);
    if (DsDefaults.bundledFonts.contains(fontFamily)) {
      return base.copyWith(fontFamily: fontFamily);
    }
    return GoogleFonts.getFont(fontFamily, textStyle: base);
  }
}

/// Preload font của tenant TRƯỚC frame đầu tiên (docs/10-customer-app.md: "nếu không sẽ
/// nháy đổi font"). Gọi trong `main()`/lúc khởi tạo màn hình, trước `runApp`/trước khi
/// storefront render, `await` xong rồi mới build UI.
///
/// KHÔNG BAO GIỜ để lỗi tải font (mạng 3G rớt đúng lúc mở app) chặn hay làm crash luồng
/// render storefront — preload chỉ là "nên có" (tránh nháy font), không phải điều kiện để
/// hiện màn hình. Lỗi bị nuốt có chủ đích, khác các lỗi khác trong app.
Future<void> preloadFont(String fontFamily) async {
  if (DsDefaults.bundledFonts.contains(fontFamily)) return; // đã có sẵn trong app, không cần tải
  try {
    GoogleFonts.getFont(fontFamily); // kích hoạt tải — thêm một future vào pendingFonts()
    await GoogleFonts.pendingFonts();
  } catch (_) {
    // Tải thất bại (mất mạng, DNS…) — chấp nhận nháy font một lần, KHÔNG chặn storefront.
  }
}
