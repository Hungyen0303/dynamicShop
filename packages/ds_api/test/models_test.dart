import 'package:flutter_test/flutter_test.dart';
import 'package:ds_api/ds_api.dart';

void main() {
  group('PublicProduct — khớp PublicProductDto thật', () {
    test('parse đúng field, price là int (đồng), không double', () {
      final p = PublicProduct.fromJson(const {
        'id': '11111111-1111-1111-1111-100000000011',
        'name': 'Bún bò Huế',
        'price': 45000,
        'imageUrl': null,
        'available': true,
      });
      expect(p.name, 'Bún bò Huế');
      expect(p.price, 45000);
      expect(p.price, isA<int>());
      expect(p.imageUrl, isNull);
      expect(p.available, isTrue);
    });

    test('field thiếu vẫn parse được (forward compat)', () {
      final p = PublicProduct.fromJson(const {});
      expect(p.id, '');
      expect(p.price, 0);
      expect(p.available, isTrue); // default an toàn — không ẩn món khi thiếu field
    });
  });

  group('PublicCategory', () {
    test('parse đúng field thật', () {
      final c = PublicCategory.fromJson(const {
        'id': '11111111-1111-1111-1111-100000000001',
        'name': 'Bún',
        'sortOrder': 1,
      });
      expect(c.name, 'Bún');
      expect(c.sortOrder, 1);
    });
  });

  group('StorefrontResponse', () {
    test('giữ layout/data thô — không parse tiếp thành block ở tầng này', () {
      final res = StorefrontResponse.fromJson(const {
        'layout': {
          'schema_version': 3,
          'theme': {'primary': '#E23744'},
        },
        'data': {
          'categories': [
            {'id': 'c1', 'name': 'Bún', 'sortOrder': 1},
          ],
        },
      });
      expect(res.layout['schema_version'], 3);
      expect(res.data['categories'], hasLength(1));
    });
  });

  group('CreateOrderRequest', () {
    test('toJson() khớp CreateOrderRequest backend', () {
      const req = CreateOrderRequest(
        items: [CreateOrderItemRequest(productId: 'p1', qty: 2)],
        note: 'ít cay',
        deliveryAddress: '123 Lê Lợi',
        phone: '0901000001',
      );
      final json = req.toJson();
      expect(json['items'], [
        {'productId': 'p1', 'qty': 2},
      ]);
      expect(json['note'], 'ít cay');
    });
  });

  group('OrderResponse — khớp OrderResponseDto thật', () {
    test('parse đủ field, orderStatus/paymentStatus tách rời nhau', () {
      final res = OrderResponse.fromJson(const {
        'id': 'o1',
        'code': 'DH001',
        'orderStatus': 'PENDING',
        'paymentStatus': 'UNPAID',
        'subtotal': 45000,
        'shippingFee': 0,
        'discount': 0,
        'total': 45000,
        'note': null,
        'deliveryAddress': null,
        'phone': '0901000001',
        'items': [
          {'nameSnapshot': 'Bún bò Huế', 'unitPrice': 45000, 'qty': 1, 'lineTotal': 45000},
        ],
        'createdAt': '2026-08-22T10:00:00Z',
      });
      expect(res.orderStatus, 'PENDING');
      expect(res.paymentStatus, 'UNPAID');
      expect(res.total, 45000);
      expect(res.items.single.nameSnapshot, 'Bún bò Huế');
      expect(res.createdAt, isNotNull);
    });

    test('createdAt sai format không throw, trả null', () {
      final res = OrderResponse.fromJson(const {'createdAt': 'khong-phai-ngay-thang'});
      expect(res.createdAt, isNull);
    });
  });
}
