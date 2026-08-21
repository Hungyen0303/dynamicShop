import 'main_dev.dart' as main_dev;

/// Entry point mặc định khi chạy `flutter run` không kèm `--flavor`/`-t` — trỏ về `dev`
/// cho tiện lúc phát triển. Build thật LUÔN chỉ định rõ flavor:
/// `flutter run --flavor dev -t lib/main_dev.dart` (xem apps/customer_app/docs/flavors.md).
void main() => main_dev.main();
