/// Khớp `CreateOrderItemRequest` (backend). `options` là tuỳ chọn món (size, topping…),
/// tự do theo từng shop nên giữ `Map<String, dynamic>?` thô thay vì model cứng.
class CreateOrderItemRequest {
  const CreateOrderItemRequest({required this.productId, required this.qty, this.options});

  final String productId;
  final int qty;
  final Map<String, dynamic>? options;

  Map<String, dynamic> toJson() => {
        'productId': productId,
        'qty': qty,
        if (options != null) 'options': options,
      };
}

/// Khớp `CreateOrderRequest`. `POST /v1/s/{slug}/orders` — luôn kèm header
/// `Idempotency-Key` (xem `ApiClient.createOrder`), không phải field trong body này.
class CreateOrderRequest {
  const CreateOrderRequest({
    required this.items,
    this.note,
    this.deliveryAddress,
    this.phone,
  });

  final List<CreateOrderItemRequest> items;
  final String? note;
  final String? deliveryAddress;
  final String? phone;

  Map<String, dynamic> toJson() => {
        'items': items.map((e) => e.toJson()).toList(),
        'note': note,
        'deliveryAddress': deliveryAddress,
        'phone': phone,
      };
}
