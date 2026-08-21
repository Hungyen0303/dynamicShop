import 'package:flutter_test/flutter_test.dart';
import 'package:ds_core/ds_core.dart';

void main() {
  group('Json — parse khoan dung', () {
    test('str/dbl/integer/bl trả null/default khi field thiếu, không throw', () {
      final json = <String, dynamic>{};
      expect(json.str('name'), isNull);
      expect(json.dbl('price'), isNull);
      expect(json.integer('qty'), isNull);
      expect(json.bl('available'), isFalse);
      expect(json.bl('available', or: true), isTrue);
    });

    test('dbl nhận cả int lẫn double — server có thể gửi 2 thay vì 2.0', () {
      expect({'x': 2}.dbl('x'), 2.0);
      expect({'x': 2.5}.dbl('x'), 2.5);
    });

    test('sai kiểu rơi về giá trị an toàn thay vì throw', () {
      final json = <String, dynamic>{'name': 123, 'price': 'not a number', 'available': 'yes'};
      expect(json.str('name'), isNull);
      expect(json.dbl('price'), isNull);
      expect(json.bl('available', or: true), isTrue); // sai kiểu -> dùng default, không throw
    });

    test('list/obj khoan dung khi field thiếu hoặc sai kiểu', () {
      final json = <String, dynamic>{'items': 'not a list', 'meta': 42};
      expect(json.list('items'), isEmpty);
      expect(json.obj('meta'), isEmpty);
    });

    test('obj không đụng Map.map() có sẵn của Dart', () {
      final json = <String, dynamic>{
        'theme': {'primary': '#E23744'},
      };
      expect(json.obj('theme'), {'primary': '#E23744'});
    });
  });
}
