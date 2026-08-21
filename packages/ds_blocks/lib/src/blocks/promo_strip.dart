import 'package:flutter/material.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../block_node.dart';
import '../block_scope.dart';
import '../style_resolver.dart';

/// `contracts/blocks.registry.json#/blocks/promo_strip`. Props: `text` (default ''),
/// `bg_token` (default 'primary'), `dismissible` (default true). Không cho override màu
/// chữ — tự suy từ luminance của `bg` (`$note` trong registry).
Widget buildPromoStrip(BuildContext context, BlockNode node, BlockScope scope) {
  final props = node.props;
  final text = props.str('text');
  if (text == null || text.isEmpty) return const SizedBox.shrink();

  final resolver = StyleResolver(node.overrides);
  final baseToken = props.str('bg_token') ?? 'primary';
  final baseColor = StyleResolver.colorToken(context.tokens, baseToken) ?? context.tokens.primary;
  final bg = resolver.background(context, variant: baseColor);
  final radius = resolver.radius(context);
  final dismissible = props.bl('dismissible', or: true);

  return _PromoStrip(text: text, bg: bg, radius: radius, dismissible: dismissible);
}

class _PromoStrip extends StatefulWidget {
  const _PromoStrip({required this.text, required this.bg, required this.radius, required this.dismissible});

  final String text;
  final Color bg;
  final double radius;
  final bool dismissible;

  @override
  State<_PromoStrip> createState() => _PromoStripState();
}

class _PromoStripState extends State<_PromoStrip> {
  bool _dismissed = false;

  @override
  Widget build(BuildContext context) {
    if (_dismissed) return const SizedBox.shrink();
    final t = context.tokens;
    final fg = t.onColorOf(widget.bg);

    return Padding(
      padding: EdgeInsets.symmetric(horizontal: t.space(2), vertical: t.space(1)),
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: t.space(1.5), vertical: t.space(1)),
        decoration: BoxDecoration(color: widget.bg, borderRadius: BorderRadius.circular(widget.radius)),
        child: Row(
          children: [
            Expanded(
              child: Text(
                widget.text,
                style: t.textStyle(color: fg, fontWeight: FontWeight.w600),
              ),
            ),
            if (widget.dismissible)
              InkWell(
                onTap: () => setState(() => _dismissed = true),
                child: Icon(Icons.close, size: 18, color: fg),
              ),
          ],
        ),
      ),
    );
  }
}
