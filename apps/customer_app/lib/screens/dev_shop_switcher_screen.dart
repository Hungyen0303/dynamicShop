import 'package:flutter/material.dart';

/// Chỉ tồn tại ở flavor `dev` — đổi giữa 2 mock shop để test multi-tenant/SDUI bằng tay
/// (docs/70-stages.md bước 3 của flow đầu-cuối). Người dùng thật (một shop = một flavor)
/// không bao giờ thấy màn hình này.
class DevShopSwitcherScreen extends StatelessWidget {
  const DevShopSwitcherScreen({super.key, required this.currentSlug, required this.onSelected});

  final String currentSlug;
  final void Function(String slug) onSelected;

  // Hai mock shop cố định trong fixture SQL (V900/V901) — Stage 0 chưa có API "danh sách
  // shop", không cần thiết vì chỉ có 2 shop giả lập cho tới khi qua Stage 1.
  static const _mockShops = [
    (slug: 'bun-co-ba', label: 'Bún Cô Ba', subtitle: 'quán bún — theme đỏ'),
    (slug: 'tra-sua-ngoc', label: 'Trà Sữa Ngọc', subtitle: 'quán trà sữa — theme xanh ngọc'),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Đổi shop (chỉ dev)')),
      body: ListView(
        children: [
          for (final shop in _mockShops)
            ListTile(
              leading: Icon(
                shop.slug == currentSlug ? Icons.radio_button_checked : Icons.radio_button_unchecked,
              ),
              title: Text(shop.label),
              subtitle: Text(shop.subtitle),
              onTap: () => onSelected(shop.slug),
            ),
        ],
      ),
    );
  }
}
