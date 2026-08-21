import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_sdui/ds_sdui.dart';

import 'package:customer_app/data/storefront_controller.dart';
import 'package:customer_app/data/storefront_repository.dart';

/// http.Client giả lập MẤT MẠNG hoàn toàn — không có request thật nào được gửi đi, ném
/// lỗi ngay lập tức để test chạy nhanh và không phụ thuộc mạng thật.
class _OfflineHttpClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    throw const SocketExceptionStub();
  }
}

class SocketExceptionStub implements Exception {
  const SocketExceptionStub();
  @override
  String toString() => 'SocketExceptionStub: mất kết nối (giả lập cho test)';
}

void main() {
  testWidgets('chặn API hoàn toàn (không có cache) → vẫn render được từ assets/default_storefront.json',
      (tester) async {
    final offlineClient = ApiClient(baseUrl: 'http://127.0.0.1:0', httpClient: _OfflineHttpClient());
    late Directory tempDir;
    late StorefrontController controller;

    // `testWidgets` chạy trong vùng "fake async" — không chỉ `Future.delayed` (backoff
    // retry của `ApiClient`) mà cả I/O thật (`dart:io`, tạo thư mục tạm) cũng không đảm
    // bảo hoàn tất nếu gọi ngoài `runAsync()` (vùng async THẬT) trong môi trường này. Gom
    // toàn bộ phần đụng I/O/mạng thật vào một khối `runAsync` duy nhất.
    await tester.runAsync(() async {
      tempDir = await Directory.systemTemp.createTemp('ds_cache_test_');
      final repository = StorefrontRepository(
        apiClient: offlineClient,
        cache: StorefrontDiskCache(directoryProvider: () async => tempDir),
      );
      controller = StorefrontController(repository: repository, slug: 'shop-khong-ton-tai');
      await controller.bootstrap();
    });
    addTearDown(() => tempDir.delete(recursive: true));

    // Không cache (lần đầu chạy test, chưa từng lưu gì cho slug này) + network lỗi hoàn
    // toàn → PHẢI rơi xuống tầng 3 (bundled), không được để `layout` là null.
    expect(controller.layout, isNotNull);
    expect(controller.source, StorefrontSource.bundled);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: StorefrontRenderer(layout: controller.layout!, data: controller.data ?? const {}),
      ),
    ));
    await tester.pumpAndSettle();

    // assets/default_storefront.json có info_card với title này — xác nhận có nội dung
    // thật trên màn hình, không phải màn hình trắng.
    expect(find.text('Không có kết nối'), findsOneWidget);
  });
}
