import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Format tiền đồng — luôn `long`/`int`, không bao giờ `double` (bất biến #3 AGENTS.md).
/// Không dùng package `intl` (chưa nằm trong danh sách dependency đã duyệt) — chỉ cần
/// dấu chấm ngăn cách hàng nghìn cho VND, tự viết đủ dùng.
String formatVnd(int amount) {
  final digits = amount.abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    final posFromEnd = digits.length - i;
    buffer.write(digits[i]);
    if (posFromEnd > 1 && posFromEnd % 3 == 1) buffer.write('.');
  }
  return '${amount < 0 ? '-' : ''}$buffer₫';
}

class DsPriceText extends StatelessWidget {
  const DsPriceText(this.amount, {super.key, this.strong = false, this.color});

  final int amount;
  final bool strong;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return Text(
      formatVnd(amount),
      style: t.textStyle(
        color: color ?? t.onSurface,
        fontWeight: strong ? FontWeight.w700 : FontWeight.w500,
        fontSize: 14 * t.textScaleFactor,
      ),
    );
  }
}
