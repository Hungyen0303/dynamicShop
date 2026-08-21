import 'package:flutter/material.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/info_card`. Props: `title`, `body`, `icon`
/// (tên Material icon, tuỳ chọn) — đều default rỗng.
Widget buildInfoCard(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final props = node.props;
  final title = props.str('title') ?? '';
  final body = props.str('body') ?? '';
  if (title.isEmpty && body.isEmpty) return const SizedBox.shrink();

  final resolver = StyleResolver(node.overrides);
  final radius = resolver.radius(context);
  final bg = resolver.background(context);
  final fg = t.onColorOf(bg);

  return Padding(
    padding: EdgeInsets.all(t.space(2)),
    child: Container(
      padding: EdgeInsets.all(t.space(2)),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(radius)),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.storefront, color: fg),
          SizedBox(width: t.space(1.5)),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (title.isNotEmpty)
                  Text(
                    title,
                    style: t.textStyle(color: fg, fontWeight: FontWeight.w700, fontSize: 16),
                  ),
                if (body.isNotEmpty)
                  Padding(
                    padding: EdgeInsets.only(top: t.space(0.5)),
                    child: Text(body, style: t.textStyle(color: fg)),
                  ),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}
