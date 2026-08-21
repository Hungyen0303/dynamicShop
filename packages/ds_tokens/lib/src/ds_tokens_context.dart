import 'package:flutter/material.dart';

import 'ds_tokens.dart';

/// `context.tokens` — cách duy nhất component đọc token (docs/60-design.md bất biến #2).
/// Không bao giờ `!` — nếu vì lý do gì đó `MaterialApp` chưa gắn `DsTokens` vào theme
/// (test widget đơn lẻ, ví dụ), rơi về `DsTokens.appDefault()` thay vì crash.
extension DsTokensContext on BuildContext {
  DsTokens get tokens => Theme.of(this).extension<DsTokens>() ?? DsTokens.appDefault();
}
