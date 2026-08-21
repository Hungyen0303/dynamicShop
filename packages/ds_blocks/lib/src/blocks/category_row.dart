import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../sdui_action.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/category_row`. Props: `style` (chip|circle,
/// default chip), `show_all` (default true). `data_ref` cố định `"categories"`.
Widget buildCategoryRow(BuildContext context, BlockNode node, BlockScope scope) {
  final t = context.tokens;
  final resolver = StyleResolver(node.overrides);
  final props = node.props;

  final style = resolver.overrideString('style') ?? props.str('style') ?? 'chip';
  final showAll = props.bl('show_all', or: true);
  final bg = resolver.background(context, variant: t.surface);

  final categories =
      scope.data is List<PublicCategory> ? scope.data as List<PublicCategory> : const <PublicCategory>[];
  if (categories.isEmpty) return const SizedBox.shrink();

  return Container(
    color: bg,
    padding: EdgeInsets.symmetric(vertical: t.space(1)),
    child: SizedBox(
      height: style == 'circle' ? 76 : 40,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: EdgeInsets.symmetric(horizontal: t.space(2)),
        children: [
          if (showAll)
            _CategoryItem(
              label: 'Tất cả',
              circle: style == 'circle',
              onTap: () => scope.onAction?.call(const SduiAction(type: SduiActionType.openCategory, categoryId: null)),
            ),
          for (final category in categories)
            _CategoryItem(
              label: category.name,
              circle: style == 'circle',
              onTap: () =>
                  scope.onAction?.call(SduiAction(type: SduiActionType.openCategory, categoryId: category.id)),
            ),
        ],
      ),
    ),
  );
}

class _CategoryItem extends StatelessWidget {
  const _CategoryItem({required this.label, required this.circle, this.onTap});

  final String label;
  final bool circle;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final trimmed = label.trim();
    final initial = trimmed.isEmpty ? '?' : trimmed.substring(0, 1).toUpperCase();

    return Padding(
      padding: EdgeInsets.only(right: t.space(1)),
      child: InkWell(
        borderRadius: BorderRadius.circular(t.radiusLg),
        onTap: onTap,
        child: circle
            ? Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  CircleAvatar(
                    radius: 24,
                    backgroundColor: t.primary,
                    child: Text(initial, style: t.textStyle(color: t.onPrimary, fontWeight: FontWeight.w700)),
                  ),
                  SizedBox(height: t.space(0.5)),
                  Text(label, style: t.textStyle(fontSize: 12, color: t.onSurface)),
                ],
              )
            : Container(
                padding: EdgeInsets.symmetric(horizontal: t.space(1.5), vertical: t.space(0.75)),
                decoration: BoxDecoration(
                  color: t.surface,
                  border: Border.all(color: t.border),
                  borderRadius: BorderRadius.circular(t.radiusLg),
                ),
                alignment: Alignment.center,
                child: Text(
                  label,
                  style: t.textStyle(fontWeight: FontWeight.w600, color: t.onSurface),
                ),
              ),
      ),
    );
  }
}
