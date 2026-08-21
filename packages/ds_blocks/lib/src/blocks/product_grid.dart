import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../product_card_style.dart';
import '../sdui_action.dart';
import '../style_resolver.dart';
import 'product_card.dart';

/// `contracts/blocks.registry.json#/blocks/product_grid`. Props: `columns` (2|3, default
/// 2), `card_style` (preset, default elevated), `show_price` (default true).
Widget buildProductGrid(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final resolver = StyleResolver(node.overrides);
  final props = node.props;

  final columns = resolver.overrideDouble('columns')?.round() ?? props.integer('columns') ?? 2;
  final style = ProductCardStyle.of(resolver.overrideString('card_style') ?? props.str('card_style'));
  final showPrice = props.bl('show_price', or: true);

  final products = scope.data is List<PublicProduct> ? scope.data as List<PublicProduct> : const <PublicProduct>[];

  if (products.isEmpty) return const SizedBox.shrink();

  return Padding(
    padding: EdgeInsets.symmetric(horizontal: t.space(2)),
    child: GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: products.length,
      gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: columns,
        crossAxisSpacing: t.space(1),
        mainAxisSpacing: t.space(1),
        childAspectRatio: 0.72,
      ),
      itemBuilder: (context, index) {
        final product = products[index];
        return ProductCard(
          product: product,
          style: style,
          resolver: resolver,
          showPrice: showPrice,
          onTap: () => scope.onAction?.call(SduiAction(type: SduiActionType.openProduct, productId: product.id)),
        );
      },
    ),
  );
}
