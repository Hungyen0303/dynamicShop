import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../product_card_style.dart';
import '../style_resolver.dart';

/// Thẻ món dùng chung cho `product_grid` và `product_list` — một widget, N preset
/// (docs/60-design.md "Variant = preset, không phải class"). KHÔNG tạo
/// `MinimalProductCard`/`ElevatedProductCard`… riêng.
class ProductCard extends StatelessWidget {
  const ProductCard({
    super.key,
    required this.product,
    required this.style,
    required this.resolver,
    this.showPrice = true,
    this.dense = false,
    this.onTap,
  });

  final PublicProduct product;
  final ProductCardStyle style;
  final StyleResolver resolver;
  final bool showPrice;
  final bool dense;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final radius = resolver.radius(context, variant: style.radius);
    final bg = resolver.background(context, variant: t.surface);

    final image = DsNetworkImage(url: product.imageUrl, aspectRatio: 1, radius: style.overlayText ? radius : 0);

    Widget body;
    if (style.overlayText) {
      body = Stack(
        children: [
          image,
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(radius),
                gradient: DsScrim.gradient(alpha: 0.65),
              ),
            ),
          ),
          Positioned(
            left: t.space(1),
            right: t.space(1),
            bottom: t.space(1),
            child: _Labels(product: product, showPrice: showPrice, color: DsScrim.overlayText, dense: dense),
          ),
        ],
      );
    } else {
      body = Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          image,
          Padding(
            padding: EdgeInsets.all(t.space(1)),
            child: _Labels(product: product, showPrice: showPrice, color: t.onSurface, dense: dense),
          ),
        ],
      );
    }

    return DsCard(
      radius: radius,
      background: bg,
      showBorder: style.showBorder,
      elevation: style.elevation,
      child: InkWell(
        onTap: product.available ? onTap : null,
        child: Opacity(opacity: product.available ? 1.0 : 0.5, child: body),
      ),
    );
  }
}

class _Labels extends StatelessWidget {
  const _Labels({required this.product, required this.showPrice, required this.color, required this.dense});

  final PublicProduct product;
  final bool showPrice;
  final Color color;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          product.name,
          maxLines: dense ? 1 : 2,
          overflow: TextOverflow.ellipsis,
          style: t.textStyle(color: color, fontWeight: FontWeight.w600, fontSize: 14 * t.textScaleFactor),
        ),
        if (!product.available)
          Text(
            'Hết món',
            style: t.textStyle(color: t.danger, fontSize: 12),
          )
        else if (showPrice)
          Padding(
            padding: EdgeInsets.only(top: t.space(0.5)),
            child: DsPriceText(product.price, strong: true, color: color),
          ),
      ],
    );
  }
}
