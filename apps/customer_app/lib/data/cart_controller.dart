import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:ds_core/ds_core.dart';

import '../models/cart_item.dart';

/// Giỏ hàng — HARDCODE (bất biến #5). Giữ riêng theo từng `slug` (đổi shop ở màn dev
/// không làm lẫn giỏ hàng của shop khác). Lưu local qua `KvStore` — "giỏ hàng lưu local,
/// không mất khi mất mạng" (docs/10-customer-app.md mục "Mạng yếu").
class CartController extends ChangeNotifier {
  CartController({required String slug, KvStore? kvStore}) : _slug = slug, _kv = kvStore ?? const KvStore();

  String _slug;
  final KvStore _kv;
  List<CartItem> _items = [];

  List<CartItem> get items => List.unmodifiable(_items);
  int get totalQty => _items.fold(0, (sum, i) => sum + i.qty);
  int get subtotal => _items.fold(0, (sum, i) => sum + i.lineTotal);
  bool get isEmpty => _items.isEmpty;

  String _keyFor(String slug) => 'cart_$slug';

  Future<void> load() async {
    final raw = await _kv.getString(_keyFor(_slug));
    if (raw == null) {
      _items = [];
      notifyListeners();
      return;
    }
    try {
      final decoded = jsonDecode(raw);
      _items = decoded is List
          ? decoded
              .whereType<Map>()
              .map((e) => CartItem.fromJson(Map<String, dynamic>.from(e)))
              .whereType<CartItem>()
              .toList()
          : [];
    } catch (_) {
      _items = []; // giỏ hàng lưu hỏng không được làm app crash lúc mở lại
    }
    notifyListeners();
  }

  Future<void> switchSlug(String newSlug) async {
    if (newSlug == _slug) return;
    _slug = newSlug;
    await load();
  }

  Future<void> add({
    required String productId,
    required String name,
    required int unitPrice,
    required String? imageUrl,
    int qty = 1,
    Map<String, dynamic>? options,
  }) async {
    final index = _items.indexWhere((i) => i.productId == productId);
    if (index >= 0) {
      _items[index] = _items[index].copyWith(qty: _items[index].qty + qty);
    } else {
      _items.add(CartItem(productId: productId, name: name, unitPrice: unitPrice, qty: qty, imageUrl: imageUrl, options: options));
    }
    await _persist();
  }

  Future<void> updateQty(String productId, int qty) async {
    if (qty <= 0) {
      await remove(productId);
      return;
    }
    final index = _items.indexWhere((i) => i.productId == productId);
    if (index < 0) return;
    _items[index] = _items[index].copyWith(qty: qty);
    await _persist();
  }

  Future<void> remove(String productId) async {
    _items.removeWhere((i) => i.productId == productId);
    await _persist();
  }

  Future<void> clear() async {
    _items = [];
    await _persist();
  }

  Future<void> _persist() async {
    await _kv.setString(_keyFor(_slug), jsonEncode(_items.map((e) => e.toJson()).toList()));
    notifyListeners();
  }
}
