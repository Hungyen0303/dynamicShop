import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_tokens/ds_tokens.dart';

import '../data/cart_controller.dart';
import '../data/order_repository.dart';
import 'order_confirmation_screen.dart';

/// Checkout — HARDCODE, không SDUI (bất biến #5). `Idempotency-Key` do
/// [OrderRepository] tự sinh và GIỮ NGUYÊN nếu bấm "Đặt hàng" lại sau lỗi mạng — bấm lại
/// nhiều lần cho cùng giỏ hàng KHÔNG tạo đơn thứ hai.
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key, required this.cartController, required this.orderRepository, required this.slug});

  final CartController cartController;
  final OrderRepository orderRepository;
  final String slug;

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _noteController = TextEditingController();
  final _addressController = TextEditingController();
  final _phoneController = TextEditingController();
  bool _submitting = false;
  String? _error;

  @override
  void dispose() {
    _noteController.dispose();
    _addressController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final request = CreateOrderRequest(
        items: widget.cartController.items
            .map((i) => CreateOrderItemRequest(productId: i.productId, qty: i.qty, options: i.options))
            .toList(),
        note: _noteController.text.trim().isEmpty ? null : _noteController.text.trim(),
        deliveryAddress: _addressController.text.trim().isEmpty ? null : _addressController.text.trim(),
        phone: _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
      );
      final order = await widget.orderRepository.submit(widget.slug, request);
      await widget.cartController.clear();
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => OrderConfirmationScreen(order: order)),
      );
    } on ApiHttpException catch (e) {
      setState(() => _error = e.message);
    } on ApiTimeoutException {
      setState(() => _error = 'Quá thời gian chờ — kiểm tra mạng rồi thử lại. Đơn của bạn chưa bị gửi trùng.');
    } on ApiNetworkException {
      setState(() => _error = 'Không kết nối được máy chủ — kiểm tra mạng rồi thử lại.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    final cart = widget.cartController;

    return Scaffold(
      appBar: AppBar(title: const Text('Thanh toán')),
      body: ListView(
        padding: EdgeInsets.all(t.space(2)),
        children: [
          for (final item in cart.items)
            Padding(
              padding: EdgeInsets.only(bottom: t.space(1)),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(child: Text('${item.name} × ${item.qty}', style: t.textStyle())),
                  DsPriceText(item.lineTotal),
                ],
              ),
            ),
          Divider(color: t.border, height: t.space(4)),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Tổng cộng', style: t.textStyle(fontWeight: FontWeight.w700, fontSize: 16)),
              DsPriceText(cart.subtotal, strong: true),
            ],
          ),
          SizedBox(height: t.space(3)),
          DsTextField(label: 'Địa chỉ giao hàng', controller: _addressController),
          SizedBox(height: t.space(1.5)),
          DsTextField(label: 'Số điện thoại', controller: _phoneController, keyboardType: TextInputType.phone),
          SizedBox(height: t.space(1.5)),
          DsTextField(label: 'Ghi chú', controller: _noteController, maxLines: 3),
          SizedBox(height: t.space(3)),
          if (_error != null)
            Padding(
              padding: EdgeInsets.only(bottom: t.space(2)),
              child: Text(_error!, style: t.textStyle(color: t.danger)),
            ),
          DsButton(
            label: _submitting ? 'Đang gửi…' : 'Xác nhận đặt hàng',
            expand: true,
            onPressed: _submitting || cart.isEmpty ? null : _submit,
          ),
        ],
      ),
    );
  }
}
