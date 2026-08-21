import 'package:flutter/material.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../sdui_action.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/image_banner`. Props: `image_url` (default
/// ''), `action` (default null). Không có `aspect_ratio` riêng — luôn banner ngang 2:1
/// (docs/60-design.md "Tỉ lệ chuẩn").
Widget buildImageBanner(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final props = node.props;
  final imageUrl = props.str('image_url');
  final resolver = StyleResolver(node.overrides);
  final radius = resolver.radius(context);
  final action = SduiAction.fromProp(props['action']);

  final content = Padding(
    padding: EdgeInsets.symmetric(horizontal: t.space(2), vertical: t.space(1)),
    child: DsNetworkImage(url: imageUrl, aspectRatio: 2.0, radius: radius),
  );

  if (action == null) return content;
  return GestureDetector(onTap: () => scope.onAction?.call(action), child: content);
}
