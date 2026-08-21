import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Nhãn trạng thái đơn — dùng ở màn theo dõi đơn (hardcode). Tone là preset ánh xạ tới
/// token màu, không nhận `Color` tự do.
enum DsBadgeTone { neutral, success, warning, danger }

class DsStatusBadge extends StatelessWidget {
  const DsStatusBadge({super.key, required this.label, this.tone = DsBadgeTone.neutral});

  final String label;
  final DsBadgeTone tone;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final bg = switch (tone) {
      DsBadgeTone.neutral => t.border,
      DsBadgeTone.success => t.success,
      DsBadgeTone.warning => t.warning,
      DsBadgeTone.danger => t.danger,
    };
    final fg = tone == DsBadgeTone.neutral ? t.onSurface : t.onColorOf(bg);

    return Container(
      padding: EdgeInsets.symmetric(horizontal: t.space(1.25), vertical: t.space(0.5)),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(t.radiusSm)),
      child: Text(
        label,
        style: t.textStyle(color: fg, fontSize: 12, fontWeight: FontWeight.w600),
      ),
    );
  }
}
