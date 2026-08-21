import 'dart:convert';

import 'package:flutter/services.dart' show rootBundle;
import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';

enum StorefrontSource { network, cache, bundled }

class StorefrontLoadResult {
  const StorefrontLoadResult({required this.layout, required this.data, required this.source});

  final Map<String, dynamic> layout;
  final Map<String, dynamic> data;
  final StorefrontSource source;
}

/// Ba tầng fallback (docs/10-customer-app.md mục "Fallback ba tầng"):
/// 1. server (fresh) — [fetchFresh]
/// 2. cache đĩa, key theo `tenant_id` (ở đây là slug, xem `StorefrontDiskCache`) +
///    `schema_version` — [loadCached]
/// 3. `assets/default_storefront.json` bundle sẵn trong app — [loadBundled]
///
/// Việc PHỐI HỢP 3 tầng (thử cache trước để render ngay, fetch nền, rơi xuống bundled khi
/// cả hai tầng trên đều thất bại) là trách nhiệm của `StorefrontController`, không phải
/// class này — repository chỉ biết cách đọc TỪNG tầng riêng lẻ.
class StorefrontRepository {
  StorefrontRepository({required this.apiClient, StorefrontDiskCache? cache, this.schemaVersion = 3})
      : _cache = cache ?? const StorefrontDiskCache();

  final ApiClient apiClient;
  final StorefrontDiskCache _cache;
  final int schemaVersion;

  Future<StorefrontLoadResult> fetchFresh(String slug) async {
    final res = await apiClient.getStorefront(slug, schema: schemaVersion);
    await _cache.save(slug, schemaVersion, jsonEncode({'layout': res.layout, 'data': res.data}));
    return StorefrontLoadResult(layout: res.layout, data: res.data, source: StorefrontSource.network);
  }

  Future<StorefrontLoadResult?> loadCached(String slug) async {
    final raw = await _cache.load(slug, schemaVersion);
    if (raw == null) return null;
    return StorefrontLoadResult(layout: raw.obj('layout'), data: raw.obj('data'), source: StorefrontSource.cache);
  }

  Future<StorefrontLoadResult> loadBundled() async {
    final raw = await rootBundle.loadString('assets/default_storefront.json');
    final decoded = jsonDecode(raw);
    final map = decoded is Map<String, dynamic> ? decoded : <String, dynamic>{};
    return StorefrontLoadResult(layout: map.obj('layout'), data: map.obj('data'), source: StorefrontSource.bundled);
  }
}
