import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ds_blocks/ds_blocks.dart';
import 'package:ds_sdui/ds_sdui.dart';

void main() {
  group('BlockRegistry — hai chốt chặn bắt buộc', () {
    testWidgets('(1) forward compat: type lạ không có trong registry render SizedBox.shrink, không crash',
        (tester) async {
      final registry = BlockRegistry();
      const node = BlockNode(id: 'x1', type: 'block_tu_tuong_lai_chua_ton_tai', v: 1, props: {});

      await tester.pumpWidget(
        MaterialApp(
          home: Builder(builder: (context) => registry.build(context, node, const BlockScope())),
        ),
      );

      expect(find.byType(SizedBox), findsOneWidget);
    });

    testWidgets('(2) error boundary: builder ném lỗi vẫn render SizedBox.shrink, không crash cả cây', (tester) async {
      final registry = BlockRegistry(builders: {
        'boom': (context, node, scope) => throw StateError('cố ý lỗi để test error boundary'),
      });
      const node = BlockNode(id: 'x2', type: 'boom', v: 1, props: {});

      await tester.pumpWidget(
        MaterialApp(
          home: Column(
            children: [
              Builder(builder: (context) => registry.build(context, node, const BlockScope())),
              const Text('vẫn render được phần còn lại của màn hình'),
            ],
          ),
        ),
      );

      expect(find.text('vẫn render được phần còn lại của màn hình'), findsOneWidget);
    });

    test('registeredTypes khớp đúng 8 block trong contracts/blocks.registry.json', () {
      final registry = BlockRegistry();
      expect(registry.registeredTypes, {
        'hero_banner',
        'category_row',
        'product_grid',
        'product_list',
        'promo_strip',
        'info_card',
        'image_banner',
        'spacer',
      });
    });
  });
}
