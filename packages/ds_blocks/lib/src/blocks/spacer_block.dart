import 'package:flutter/material.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/spacer`. Prop: `height_units` (default 2) —
/// nhân với `spacingUnit` (8), KHÔNG phải pixel tự do (mọi spacing là bội số của unit).
Widget buildSpacer(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final resolver = StyleResolver(node.overrides);
  final baseUnits = node.props.dbl('height_units') ?? 2;
  final units = resolver.overrideDouble('height') ?? baseUnits;
  return SizedBox(height: t.space(units));
}
