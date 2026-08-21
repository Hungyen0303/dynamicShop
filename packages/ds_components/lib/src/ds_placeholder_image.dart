import 'package:flutter/material.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Ảnh sản phẩm/banner. `imageUrl` có thể `null`/rỗng (menu shop chưa có ảnh) hoặc tải
/// lỗi (mạng 3G) — cả hai trường hợp phải luôn có placeholder, không bao giờ crash hay để
/// khoảng trắng vô nghĩa (docs/60-design.md mục "Ảnh").
class DsNetworkImage extends StatelessWidget {
  const DsNetworkImage({
    super.key,
    required this.url,
    this.aspectRatio = 1.0,
    this.radius,
    this.fit = BoxFit.cover,
  });

  final String? url;
  final double aspectRatio;
  final double? radius;
  final BoxFit fit;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final r = radius ?? t.radiusMd;

    Widget image;
    if (url == null || url!.isEmpty) {
      image = _placeholder(t);
    } else {
      image = Image.network(
        url!,
        fit: fit,
        errorBuilder: (context, error, stack) => _placeholder(t),
        loadingBuilder: (context, child, progress) {
          if (progress == null) return child;
          return _placeholder(t);
        },
      );
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(r),
      child: AspectRatio(aspectRatio: aspectRatio, child: image),
    );
  }

  Widget _placeholder(DsTokens t) => Container(
        color: t.border,
        alignment: Alignment.center,
        child: Icon(Icons.restaurant_menu, color: t.muted, size: 28),
      );
}
