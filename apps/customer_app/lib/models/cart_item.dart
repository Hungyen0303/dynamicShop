/// Giỏ hàng — HARDCODE, không SDUI (bất biến #5 docs/10-customer-app.md). Snapshot
/// `name`/`price` lúc thêm vào giỏ, giống tinh thần snapshot của `order_items` bên
/// backend (bất biến #4 AGENTS.md) — giá đổi ở server sau đó không làm giỏ hàng đang mở
/// hiển thị sai.
class CartItem {
  const CartItem({
    required this.productId,
    required this.name,
    required this.unitPrice,
    required this.qty,
    this.imageUrl,
    this.options,
  });

  final String productId;
  final String name;
  final int unitPrice;
  final int qty;
  final String? imageUrl;
  final Map<String, dynamic>? options;

  int get lineTotal => unitPrice * qty;

  CartItem copyWith({int? qty}) => CartItem(
        productId: productId,
        name: name,
        unitPrice: unitPrice,
        qty: qty ?? this.qty,
        imageUrl: imageUrl,
        options: options,
      );

  Map<String, dynamic> toJson() => {
        'productId': productId,
        'name': name,
        'unitPrice': unitPrice,
        'qty': qty,
        'imageUrl': imageUrl,
        'options': options,
      };

  static CartItem? fromJson(Map<String, dynamic> json) {
    final productId = json['productId'];
    final name = json['name'];
    if (productId is! String || name is! String) return null;
    final unitPrice = json['unitPrice'];
    final qty = json['qty'];
    return CartItem(
      productId: productId,
      name: name,
      unitPrice: unitPrice is num ? unitPrice.toInt() : 0,
      qty: qty is num ? qty.toInt() : 1,
      imageUrl: json['imageUrl'] is String ? json['imageUrl'] as String : null,
      options: json['options'] is Map ? Map<String, dynamic>.from(json['options'] as Map) : null,
    );
  }
}
