import 'package:ds_core/ds_core.dart';

/// Khớp `OrderItemResponseDto` — snapshot lúc đặt, không join lại `products` (bất biến #4
/// AGENTS.md). `unitPrice`/`lineTotal` là `long` bên backend → Dart `int`.
class OrderItemResponse {
  const OrderItemResponse({
    required this.nameSnapshot,
    required this.unitPrice,
    required this.qty,
    required this.lineTotal,
  });

  factory OrderItemResponse.fromJson(Map<String, dynamic> json) => OrderItemResponse(
        nameSnapshot: json.str('nameSnapshot') ?? '',
        unitPrice: json.integer('unitPrice') ?? 0,
        qty: json.integer('qty') ?? 0,
        lineTotal: json.integer('lineTotal') ?? 0,
      );

  final String nameSnapshot;
  final int unitPrice;
  final int qty;
  final int lineTotal;
}

/// Khớp `OrderResponseDto`. `orderStatus`/`paymentStatus` giữ nguyên dạng `String` (tên
/// enum Java) — bất biến #5: hai state tách rời nhau, không gộp.
class OrderResponse {
  const OrderResponse({
    required this.id,
    required this.code,
    required this.orderStatus,
    required this.paymentStatus,
    required this.subtotal,
    required this.shippingFee,
    required this.discount,
    required this.total,
    required this.note,
    required this.deliveryAddress,
    required this.phone,
    required this.items,
    required this.createdAt,
  });

  factory OrderResponse.fromJson(Map<String, dynamic> json) => OrderResponse(
        id: json.str('id') ?? '',
        code: json.str('code') ?? '',
        orderStatus: json.str('orderStatus') ?? 'PENDING',
        paymentStatus: json.str('paymentStatus') ?? 'UNPAID',
        subtotal: json.integer('subtotal') ?? 0,
        shippingFee: json.integer('shippingFee') ?? 0,
        discount: json.integer('discount') ?? 0,
        total: json.integer('total') ?? 0,
        note: json.str('note'),
        deliveryAddress: json.str('deliveryAddress'),
        phone: json.str('phone'),
        items: json.list('items').whereType<Map>().map((e) => OrderItemResponse.fromJson(Map<String, dynamic>.from(e))).toList(),
        createdAt: DateTime.tryParse(json.str('createdAt') ?? ''),
      );

  final String id;
  final String code;
  final String orderStatus;
  final String paymentStatus;
  final int subtotal;
  final int shippingFee;
  final int discount;
  final int total;
  final String? note;
  final String? deliveryAddress;
  final String? phone;
  final List<OrderItemResponse> items;
  final DateTime? createdAt;
}
