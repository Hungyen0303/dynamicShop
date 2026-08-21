import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../sdui_action.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/product_list`. Props: `show_thumbnail`
/// (default true), `dense` (default false). Không có `card_style` — luôn dạng hàng
/// ngang, không phải preset của `ProductCardStyle` (đó là của `product_grid`).
Widget buildProductList(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final resolver = StyleResolver(node.overrides);
  final props = node.props;

  final showThumbnail = props.bl('show_thumbnail', or: true);
  final dense = resolver.overrideBool('dense') ?? props.bl('dense', or: false);
  final radius = resolver.radius(context);
  final bg = resolver.background(context);

  final products = scope.data is List<PublicProduct> ? scope.data as List<PublicProduct> : const <PublicProduct>[];
  if (products.isEmpty) return const SizedBox.shrink();

  return Padding(
    padding: EdgeInsets.symmetric(horizontal: t.space(2), vertical: t.space(0.5)),
    child: DecoratedBox(
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(radius)),
      child: Column(
        children: [
          for (final product in products)
            _ProductRow(
              product: product,
              showThumbnail: showThumbnail,
              dense: dense,
              onTap: () => scope.onAction?.call(SduiAction(type: SduiActionType.openProduct, productId: product.id)),
            ),
        ],
      ),
    ),
  );
}

class _ProductRow extends StatelessWidget {
  const _ProductRow({required this.product, required this.showThumbnail, required this.dense, this.onTap});

  final PublicProduct product;
  final bool showThumbnail;
  final bool dense;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return InkWell(
      onTap: product.available ? onTap : null,
      child: Opacity(
        opacity: product.available ? 1.0 : 0.5,
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: dense ? t.space(0.75) : t.space(1.25)),
          child: Row(
            children: [
              if (showThumbnail) ...[
                SizedBox(
                  width: dense ? 44 : 56,
                  child: DsNetworkImage(url: product.imageUrl, aspectRatio: 1, radius: t.radiusSm),
                ),
                SizedBox(width: t.space(1.25)),
              ],
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      product.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: t.textStyle(fontWeight: FontWeight.w600, color: t.onSurface),
                    ),
                    if (!product.available)
                      Text('Hết món', style: t.textStyle(color: t.danger, fontSize: 12))
                    else
                      DsPriceText(product.price),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
