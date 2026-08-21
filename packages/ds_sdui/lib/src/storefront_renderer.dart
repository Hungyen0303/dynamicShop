import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_blocks/ds_blocks.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import 'block_registry.dart';

/// Render một `StorefrontResponse.layout` + `.data` — SDUI mức 2: danh sách block phẳng
/// (docs/10-customer-app.md mục "SDUI — mức 2"). Tự áp `DsTokens` của tenant lên
/// `Theme` cho toàn bộ cây con, để mọi block đọc đúng theme qua `context.tokens`.
///
/// `merchant_app` (Stage 2) phải dùng ĐÚNG widget này cho màn hình xem trước, không tự
/// dựng lại renderer riêng (AGENTS.md mục "Sửa một block" + docs/70-stages.md Stage 2).
class StorefrontRenderer extends StatelessWidget {
  const StorefrontRenderer({
    super.key,
    required this.layout,
    required this.data,
    this.onAction,
    this.registry,
    this.screen = 'home',
    this.padding,
  });

  final Map<String, dynamic> layout;
  final Map<String, dynamic> data;
  final void Function(SduiAction action)? onAction;
  final BlockRegistry? registry;
  final String screen;
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    final reg = registry ?? BlockRegistry();
    final theme = DsTokens.fromThemeJson(layout.obj('theme'));
    final nodes = _parseBlocks(layout, screen);

    return Theme(
      data: Theme.of(context).copyWith(extensions: <ThemeExtension<dynamic>>[theme]),
      // Builder để lấy BuildContext LÀ con của Theme vừa gắn — block build ngay bên
      // trong (không phải widget riêng) nên phải build bằng context đã thấy token mới,
      // nếu không `context.tokens` bên trong từng block sẽ đọc nhầm theme cũ của app.
      child: Builder(
        builder: (innerContext) => ListView(
          padding: padding,
          children: [
            for (final node in nodes)
              KeyedSubtree(
                key: ValueKey(node.id),
                child: reg.build(innerContext, node, _scopeFor(node)),
              ),
          ],
        ),
      ),
    );
  }

  static List<BlockNode> _parseBlocks(Map<String, dynamic> layout, String screen) {
    final screens = layout.obj('screens');
    final screenMap = screens.obj(screen);
    return screenMap
        .list('blocks')
        .whereType<Map>()
        .map((e) => BlockNode.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }

  BlockScope _scopeFor(BlockNode node) {
    final ref = node.dataRef;
    if (ref == null) return BlockScope(onAction: onAction);

    final raw = data[ref];
    if (raw is! List) return BlockScope(onAction: onAction);

    // Quy ước theo TYPE của block (không phải theo tên data_ref) — registry chỉ có 2 loại
    // data: "categories" (cố định, category_row) và "products:{category_id}" (product_grid
    // /product_list). Không cần một hệ kiểu tổng quát cho Stage 0 chỉ với 8 block.
    final resolved = switch (node.type) {
      'category_row' =>
        raw.whereType<Map>().map((e) => PublicCategory.fromJson(Map<String, dynamic>.from(e))).toList(),
      'product_grid' ||
      'product_list' =>
        raw.whereType<Map>().map((e) => PublicProduct.fromJson(Map<String, dynamic>.from(e))).toList(),
      _ => null,
    };

    return BlockScope(data: resolved, onAction: onAction);
  }
}

/// Hàm tiện lợi cùng tên nhắc tới trong AGENTS.md/docs — chỉ là alias của
/// [StorefrontRenderer] để nơi gọi (kể cả merchant_app studio sau này) không cần biết
/// tên class, chỉ cần biết "gọi renderStorefront(layout, data)".
Widget renderStorefront({
  required Map<String, dynamic> layout,
  required Map<String, dynamic> data,
  void Function(SduiAction action)? onAction,
  BlockRegistry? registry,
  String screen = 'home',
  EdgeInsetsGeometry? padding,
  Key? key,
}) {
  return StorefrontRenderer(
    key: key,
    layout: layout,
    data: data,
    onAction: onAction,
    registry: registry,
    screen: screen,
    padding: padding,
  );
}
