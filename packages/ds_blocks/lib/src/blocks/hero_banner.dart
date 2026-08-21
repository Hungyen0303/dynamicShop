import 'package:flutter/material.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../sdui_action.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/hero_banner`. Props: `image_url` (default
/// ''), `aspect_ratio` (default 1.777), `title` (default null), `action` (default null).
Widget buildHeroBanner(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final resolver = StyleResolver(node.overrides);
  final props = node.props;

  final imageUrl = props.str('image_url');
  final title = props.str('title');
  final baseAspect = props.dbl('aspect_ratio') ?? 1.777;
  // aspect_ratio override là segmented [1.0, 1.777, 2.0] — không có khái niệm variant nên
  // chuỗi rút gọn còn override ?? giá trị đã cấu hình của chính block.
  final aspect = resolver.overrideDouble('aspect_ratio') ?? baseAspect;
  final radius = resolver.radius(context);
  final action = SduiAction.fromProp(props['action']);

  Widget content = DsNetworkImage(url: imageUrl, aspectRatio: aspect, radius: radius);

  if (title != null && title.isNotEmpty) {
    content = ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      child: Stack(
        children: [
          content,
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(gradient: DsScrim.gradient(alpha: 0.55, stopStart: 0.4)),
            ),
          ),
          Positioned(
            left: t.space(2),
            right: t.space(2),
            bottom: t.space(2),
            child: Text(
              title,
              style: t.textStyle(color: DsScrim.overlayText, fontWeight: FontWeight.w700, fontSize: 20 * t.textScaleFactor),
            ),
          ),
        ],
      ),
    );
  }

  final padded = Padding(padding: EdgeInsets.all(t.space(2)), child: content);

  if (action == null) return padded;
  return GestureDetector(onTap: () => scope.onAction?.call(action), child: padded);
}
