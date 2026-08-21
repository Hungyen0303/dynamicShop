import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Card nền dùng chung. `radius`/`background` là tham số **tuỳ chọn** dành cho nơi gọi đã
/// tự resolve qua `StyleResolver` (blockOverride ?? variant ?? tenantTheme ?? appDefault)
/// — không phải chỗ để hardcode màu. Không truyền gì thì dùng token mặc định của tenant.
class DsCard extends StatelessWidget {
  const DsCard({
    super.key,
    required this.child,
    this.radius,
    this.background,
    this.showBorder = false,
    this.elevation = 0,
    this.padding,
    this.clip = true,
  });

  final Widget child;
  final double? radius;
  final Color? background;
  final bool showBorder;
  final double elevation;
  final EdgeInsetsGeometry? padding;
  final bool clip;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final r = radius ?? t.radiusMd;
    final bg = background ?? t.surface;

    // `shape` (khi có border) và `borderRadius` không dùng chung được trên Material —
    // shape đã tự mang borderRadius riêng, nên luôn đi qua đường shape để nhất quán.
    return Material(
      color: bg,
      elevation: elevation,
      clipBehavior: clip ? Clip.antiAlias : Clip.none,
      shape: RoundedRectangleBorder(
        side: showBorder ? BorderSide(color: t.border) : BorderSide.none,
        borderRadius: BorderRadius.circular(r),
      ),
      child: Padding(
        padding: padding ?? EdgeInsets.zero,
        child: child,
      ),
    );
  }
}
