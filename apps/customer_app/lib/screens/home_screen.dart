import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_blocks/ds_blocks.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_sdui/ds_sdui.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../data/cart_controller.dart';
import '../data/order_repository.dart';
import '../data/storefront_controller.dart';
import '../data/storefront_repository.dart';
import 'cart_screen.dart';
import 'dev_shop_switcher_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.storefrontController,
    required this.cartController,
    required this.orderRepository,
    this.onSwitchTenant,
  });

  final StorefrontController storefrontController;
  final CartController cartController;
  final OrderRepository orderRepository;

  /// Chỉ khác `null` ở flavor `dev` — người dùng thật (một shop = một flavor) không thấy
  /// nút này (docs/10-customer-app.md mục "Flavor").
  final Future<void> Function(String slug)? onSwitchTenant;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: storefrontController,
      builder: (context, _) => _buildScaffold(context),
    );
  }

  Widget _buildScaffold(BuildContext context) {
    final layout = storefrontController.layout;
    final data = storefrontController.data ?? const <String, dynamic>{};

    return Scaffold(
      appBar: AppBar(
        title: const Text('DynamicShop'),
        actions: [
          if (onSwitchTenant != null)
            IconButton(
              tooltip: 'Đổi shop (chỉ dev)',
              icon: const Icon(Icons.storefront_outlined),
              onPressed: () => _openDevSwitcher(context),
            ),
          AnimatedBuilder(
            animation: cartController,
            builder: (context, _) => IconButton(
              tooltip: 'Giỏ hàng',
              icon: Badge(
                label: Text('${cartController.totalQty}'),
                isLabelVisible: cartController.totalQty > 0,
                child: const Icon(Icons.shopping_cart_outlined),
              ),
              onPressed: () => _openCart(context),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          if (storefrontController.source != null &&
              storefrontController.source != StorefrontSource.network)
            _StaleBanner(source: storefrontController.source!),
          Expanded(
            child: layout == null
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: storefrontController.refresh,
                    child: StorefrontRenderer(
                      layout: layout,
                      data: data,
                      onAction: (action) =>
                          _handleAction(context, action, data),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  void _handleAction(
    BuildContext context,
    SduiAction action,
    Map<String, dynamic> data,
  ) {
    switch (action.type) {
      case SduiActionType.openProduct:
        final product = _findProduct(data, action.productId);
        if (product != null) _openProductSheet(context, product);
      case SduiActionType.openCategory:
        // Stage 0: layout của mock shop đã liệt kê tất cả danh mục thành các block
        // product_grid/product_list riêng ngay trên cùng một màn — chưa cần cuộn tới vì
        // không có nhiều danh mục. Ghi log để không im lặng bỏ qua hành vi người dùng.
        Telemetry.log('open_category_tap', {'categoryId': action.categoryId});
      case SduiActionType.externalUrl:
        Telemetry.log('external_url_tap', {'url': action.url});
    }
  }

  PublicProduct? _findProduct(Map<String, dynamic> data, String? id) {
    if (id == null) return null;
    for (final entry in data.entries) {
      if (!entry.key.startsWith('products:')) continue;
      final raw = entry.value;
      if (raw is! List) continue;
      for (final item in raw) {
        if (item is Map && item['id'] == id) {
          return PublicProduct.fromJson(Map<String, dynamic>.from(item));
        }
      }
    }
    return null;
  }

  void _openProductSheet(BuildContext context, PublicProduct product) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) =>
          _ProductSheet(product: product, cartController: cartController),
    );
  }

  void _openCart(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => CartScreen(
          cartController: cartController,
          orderRepository: orderRepository,
          slug: storefrontController.slug,
        ),
      ),
    );
  }

  void _openDevSwitcher(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => DevShopSwitcherScreen(
          currentSlug: storefrontController.slug,
          onSelected: (slug) async {
            await onSwitchTenant?.call(slug);
            if (context.mounted) Navigator.of(context).pop();
          },
        ),
      ),
    );
  }
}

class _StaleBanner extends StatelessWidget {
  const _StaleBanner({required this.source});

  final StorefrontSource source;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final label = source == StorefrontSource.cache
        ? 'Đang xem bản lưu tạm — không tải được menu mới nhất'
        : 'Không có kết nối — đang hiển thị bản dự phòng';
    return Container(
      width: double.infinity,
      color: t.warning,
      padding: EdgeInsets.symmetric(
        horizontal: t.space(2),
        vertical: t.space(1),
      ),
      child: Text(
        label,
        style: t.textStyle(color: t.onColorOf(t.warning), fontSize: 12),
      ),
    );
  }
}

class _ProductSheet extends StatefulWidget {
  const _ProductSheet({required this.product, required this.cartController});

  final PublicProduct product;
  final CartController cartController;

  @override
  State<_ProductSheet> createState() => _ProductSheetState();
}

class _ProductSheetState extends State<_ProductSheet> {
  int _qty = 1;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final product = widget.product;

    // `viewInsets.bottom` chỉ tính bàn phím — máy nav bar 3 nút (rất phổ biến trên Android
    // tầm thấp, đúng đối tượng người dùng của app) còn có `padding.bottom` cho thanh điều
    // hướng hệ thống. Thiếu `SafeArea` làm nút "Thêm vào giỏ" bị nav bar che, không bấm
    // được — lỗi thật phát hiện lúc test tay trên máy thật.
    return SafeArea(
      top: false,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          t.space(2),
          t.space(2),
          t.space(2),
          MediaQuery.of(context).viewInsets.bottom + t.space(2),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DsNetworkImage(
              url: product.imageUrl,
              aspectRatio: 16 / 9,
              radius: t.radiusMd,
            ),
            SizedBox(height: t.space(1.5)),
            Text(
              product.name,
              style: t.textStyle(fontWeight: FontWeight.w700, fontSize: 18),
            ),
            SizedBox(height: t.space(0.5)),
            DsPriceText(product.price, strong: true),
            SizedBox(height: t.space(2)),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                DsQtyStepper(
                  qty: _qty,
                  onChanged: (v) => setState(() => _qty = v),
                ),
                DsButton(
                  label: 'Thêm vào giỏ',
                  onPressed: product.available
                      ? () {
                          widget.cartController.add(
                            productId: product.id,
                            name: product.name,
                            unitPrice: product.price,
                            imageUrl: product.imageUrl,
                            qty: _qty,
                          );
                          Navigator.of(context).pop();
                        }
                      : null,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
