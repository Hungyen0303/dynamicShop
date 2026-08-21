import 'package:flutter/material.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../data/cart_controller.dart';
import '../data/order_repository.dart';
import 'checkout_screen.dart';

/// Giỏ hàng — HARDCODE, không SDUI (bất biến #5 docs/10-customer-app.md).
class CartScreen extends StatelessWidget {
  const CartScreen({super.key, required this.cartController, required this.orderRepository, required this.slug});

  final CartController cartController;
  final OrderRepository orderRepository;
  final String slug;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return Scaffold(
      appBar: AppBar(title: const Text('Giỏ hàng')),
      body: AnimatedBuilder(
        animation: cartController,
        builder: (context, _) {
          final items = cartController.items;
          if (items.isEmpty) {
            return Center(
              child: Text('Giỏ hàng đang trống', style: t.textStyle(color: t.muted)),
            );
          }
          return Column(
            children: [
              Expanded(
                child: ListView.separated(
                  padding: EdgeInsets.all(t.space(2)),
                  itemCount: items.length,
                  separatorBuilder: (_, _) => SizedBox(height: t.space(1.5)),
                  itemBuilder: (context, index) {
                    final item = items[index];
                    return Row(
                      children: [
                        SizedBox(
                          width: 56,
                          child: DsNetworkImage(url: item.imageUrl, aspectRatio: 1, radius: t.radiusSm),
                        ),
                        SizedBox(width: t.space(1.5)),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(item.name, style: t.textStyle(fontWeight: FontWeight.w600)),
                              DsPriceText(item.unitPrice),
                            ],
                          ),
                        ),
                        DsQtyStepper(
                          qty: item.qty,
                          onChanged: (v) => cartController.updateQty(item.productId, v),
                        ),
                        IconButton(
                          icon: Icon(Icons.delete_outline, color: t.danger),
                          onPressed: () => cartController.remove(item.productId),
                        ),
                      ],
                    );
                  },
                ),
              ),
              _CheckoutBar(cartController: cartController, orderRepository: orderRepository, slug: slug),
            ],
          );
        },
      ),
    );
  }
}

class _CheckoutBar extends StatelessWidget {
  const _CheckoutBar({required this.cartController, required this.orderRepository, required this.slug});

  final CartController cartController;
  final OrderRepository orderRepository;
  final String slug;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return DsCard(
      radius: 0,
      showBorder: true,
      padding: EdgeInsets.all(t.space(2)),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Tạm tính', style: t.textStyle(color: t.muted, fontSize: 12)),
                  DsPriceText(cartController.subtotal, strong: true),
                ],
              ),
            ),
            DsButton(
              label: 'Đặt hàng',
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                builder: (context) =>
                    CheckoutScreen(cartController: cartController, orderRepository: orderRepository, slug: slug),
              )),
            ),
          ],
        ),
      ),
    );
  }
}
