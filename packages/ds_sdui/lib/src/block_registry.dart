import 'package:flutter/material.dart';
import 'package:ds_blocks/ds_blocks.dart';
import 'package:ds_core/ds_core.dart';

typedef BlockBuilder = Widget Function(BuildContext context, BlockNode node, BlockScope scope);

/// Registry `type → builder` — nguồn danh sách type hợp lệ khớp
/// `contracts/blocks.registry.json#/blocks` (8 block Stage 0).
///
/// Hai chốt chặn bắt buộc (docs/10-customer-app.md mục "Registry — hai chốt chặn bắt
/// buộc"), KHÔNG được bỏ hoặc đơn giản hoá:
/// 1. forward compat — type không có trong registry → `SizedBox.shrink()` + telemetry
/// 2. error boundary — lỗi lúc build một block cụ thể → `SizedBox.shrink()` + telemetry
class BlockRegistry {
  BlockRegistry({Map<String, BlockBuilder>? builders}) : _builders = builders ?? defaultBuilders();

  final Map<String, BlockBuilder> _builders;

  static Map<String, BlockBuilder> defaultBuilders() => {
        'hero_banner': buildHeroBanner,
        'category_row': buildCategoryRow,
        'product_grid': buildProductGrid,
        'product_list': buildProductList,
        'promo_strip': buildPromoStrip,
        'info_card': buildInfoCard,
        'image_banner': buildImageBanner,
        'spacer': buildSpacer,
      };

  Set<String> get registeredTypes => _builders.keys.toSet();

  Widget build(BuildContext context, BlockNode node, BlockScope scope) {
    final builder = _builders[node.type];

    // (1) forward compat — block mới mà app cũ chưa cập nhật không được phá layout.
    if (builder == null) {
      Telemetry.unknownBlock(node.type, node.v);
      return const SizedBox.shrink();
    }

    // (2) error boundary — một block cấu hình sai không được làm trắng cả màn hình.
    try {
      return builder(context, node, scope);
    } catch (e, st) {
      Telemetry.blockRenderError(node.id, node.type, e, st);
      return const SizedBox.shrink();
    }
  }
}
