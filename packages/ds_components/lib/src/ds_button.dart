import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Variant là preset, không phải widget riêng (docs/60-design.md "Variant = preset,
/// không phải class"). Thêm variant thứ 4 = thêm một case trong `_colorsFor`, không thêm
/// file.
enum DsButtonVariant { primary, secondary, danger }

/// Nút dùng chung. Đọc màu từ `context.tokens` — không nhận `Color` qua constructor
/// (docs/60-design.md bất biến #2, ví dụ ☠️ trong doc).
class DsButton extends StatelessWidget {
  const DsButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.variant = DsButtonVariant.primary,
    this.expand = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final DsButtonVariant variant;
  final bool expand;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final (bg, fg, border) = switch (variant) {
      DsButtonVariant.primary => (t.primary, t.onPrimary, null),
      DsButtonVariant.secondary => (t.surface, t.onSurface, t.border),
      DsButtonVariant.danger => (t.danger, t.onColorOf(t.danger), null),
    };

    final child = Material(
      color: onPressed == null ? bg.withValues(alpha: 0.5) : bg,
      borderRadius: BorderRadius.circular(t.radiusMd),
      child: InkWell(
        borderRadius: BorderRadius.circular(t.radiusMd),
        onTap: onPressed,
        child: Container(
          padding: EdgeInsets.symmetric(horizontal: t.space(3), vertical: t.space(1.75)),
          decoration: border == null
              ? null
              : BoxDecoration(
                  border: Border.all(color: border),
                  borderRadius: BorderRadius.circular(t.radiusMd),
                ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: t.textStyle(color: fg, fontWeight: FontWeight.w600, fontSize: 15 * t.textScaleFactor),
          ),
        ),
      ),
    );

    return expand ? SizedBox(width: double.infinity, child: child) : child;
  }
}
