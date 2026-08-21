import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:ds_api/ds_api.dart';

class _OfflineHttpClient extends http.BaseClient {
  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) {
    throw const _StubError();
  }
}

class _StubError implements Exception {
  const _StubError();
  @override
  String toString() => 'mất mạng giả lập';
}

void main() {
  test('getStorefront với http.Client ném lỗi ngay -> throw nhanh, không treo', () async {
    final client = ApiClient(baseUrl: 'http://127.0.0.1:0', httpClient: _OfflineHttpClient());

    await expectLater(
      client.getStorefront('bun-co-ba'),
      throwsA(anything),
    );
  }, timeout: const Timeout(Duration(seconds: 15)));
}
