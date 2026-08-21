import 'package:flutter_test/flutter_test.dart';
import 'package:ds_api/ds_api.dart';

import 'package:customer_app/data/storefront_repository.dart';

void main() {
  testWidgets('loadBundled() đọc assets/default_storefront.json nhanh, không treo', (tester) async {
    final repo = StorefrontRepository(apiClient: ApiClient(baseUrl: 'http://unused'));
    final result = await repo.loadBundled();
    expect(result.layout, isNotEmpty);
  }, timeout: const Timeout(Duration(seconds: 15)));
}
