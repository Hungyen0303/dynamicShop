import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_components/ds_components.dart';
import 'package:ds_tokens/ds_tokens.dart';

/// Theo dõi đơn — HARDCODE, không SDUI (bất biến #5). Stage 0 backend chỉ có
/// `POST /v1/s/{slug}/orders` ở public plane (chưa có `GET` đơn public — theo dõi đơn
/// LIVE là việc của Stage 1+ khi có thêm endpoint). Màn này hiển thị đúng response server
/// trả về ngay sau khi tạo đơn — đủ để xác nhận đơn đã vào hệ thống.
class OrderConfirmationScreen extends StatelessWidget {
  const OrderConfirmationScreen({super.key, required this.order});

  final OrderResponse order;

  @override
  Widget build(BuildContext context) {
    final t = context.tokens;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Đặt hàng thành công'),
        automaticallyImplyLeading: false,
      ),
      body: ListView(
        padding: EdgeInsets.all(t.space(2)),
        children: [
          Icon(Icons.check_circle, color: t.success, size: 56),
          SizedBox(height: t.space(1)),
          Text('Mã đơn ${order.code}', style: t.textStyle(fontWeight: FontWeight.w700, fontSize: 18)),
          SizedBox(height: t.space(1)),
          Row(
            children: [
              DsStatusBadge(label: _orderStatusLabel(order.orderStatus), tone: _orderStatusTone(order.orderStatus)),
              SizedBox(width: t.space(1)),
              DsStatusBadge(label: _paymentStatusLabel(order.paymentStatus), tone: DsBadgeTone.neutral),
            ],
          ),
          Divider(color: t.border, height: t.space(4)),
          for (final item in order.items)
            Padding(
              padding: EdgeInsets.only(bottom: t.space(1)),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(child: Text('${item.nameSnapshot} × ${item.qty}', style: t.textStyle())),
                  DsPriceText(item.lineTotal),
                ],
              ),
            ),
          Divider(color: t.border, height: t.space(4)),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Tổng cộng', style: t.textStyle(fontWeight: FontWeight.w700)),
              DsPriceText(order.total, strong: true),
            ],
          ),
          SizedBox(height: t.space(3)),
          DsButton(
            label: 'Về trang chủ',
            expand: true,
            onPressed: () => Navigator.of(context).popUntil((route) => route.isFirst),
          ),
        ],
      ),
    );
  }

  String _orderStatusLabel(String status) => switch (status) {
        'PENDING' => 'Chờ xác nhận',
        'CONFIRMED' => 'Đã xác nhận',
        'PREPARING' => 'Đang chuẩn bị',
        'READY' => 'Sẵn sàng',
        'DELIVERING' => 'Đang giao',
        'COMPLETED' => 'Hoàn tất',
        'CANCELLED' => 'Đã huỷ',
        'FAILED' => 'Thất bại',
        _ => status,
      };

  DsBadgeTone _orderStatusTone(String status) => switch (status) {
        'COMPLETED' => DsBadgeTone.success,
        'CANCELLED' || 'FAILED' => DsBadgeTone.danger,
        'PENDING' => DsBadgeTone.warning,
        _ => DsBadgeTone.neutral,
      };

  String _paymentStatusLabel(String status) => switch (status) {
        'UNPAID' => 'Chưa thanh toán',
        'PARTIAL' => 'Thanh toán một phần',
        'PAID' => 'Đã thanh toán',
        'REFUNDED' => 'Đã hoàn tiền',
        _ => status,
      };
}
