import 'package:flutter/material.dart';
import 'package:ds_api/ds_api.dart';
import 'package:ds_core/ds_core.dart';
import 'package:ds_tokens/ds_tokens.dart';

import 'config/app_config.dart';
import 'data/cart_controller.dart';
import 'data/order_repository.dart';
import 'data/storefront_controller.dart';
import 'data/storefront_repository.dart';
import 'data/tenant_session.dart';
import 'screens/home_screen.dart';

class CustomerApp extends StatefulWidget {
  const CustomerApp({super.key});

  @override
  State<CustomerApp> createState() => _CustomerAppState();
}

class _CustomerAppState extends State<CustomerApp> {
  final _tenantSession = TenantSession();
  late final ApiClient _apiClient;
  StorefrontController? _storefrontController;
  CartController? _cartController;
  late final OrderRepository _orderRepository;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient(baseUrl: AppConfig.instance.apiBaseUrl);
    _orderRepository = OrderRepository(apiClient: _apiClient);
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    final slug = await _tenantSession.currentSlug();
    final storefront = StorefrontController(
      repository: StorefrontRepository(apiClient: _apiClient),
      slug: slug,
    );
    final cart = CartController(slug: slug);
    // Không await bootstrap() ở đây — nó tự notifyListeners() từng bước (cache trước,
    // network sau), HomeScreen lắng nghe và render lại đúng lúc dữ liệu tới, không cần
    // đợi ở màn splash (docs/10-customer-app.md mục "Mạng yếu": không spinner toàn màn
    // hình khi đã có cache).
    unawaited(storefront.bootstrap());
    unawaited(cart.load());

    if (!mounted) return;
    setState(() {
      _storefrontController = storefront;
      _cartController = cart;
    });
  }

  Future<void> _switchTenant(String slug) async {
    await _tenantSession.setSlug(slug);
    await _storefrontController?.switchSlug(slug);
    await _cartController?.switchSlug(slug);
  }

  @override
  void dispose() {
    _apiClient.close();
    _storefrontController?.dispose();
    _cartController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final storefront = _storefrontController;
    final cart = _cartController;
    if (storefront == null || cart == null) {
      return const _BootSplash();
    }

    // AnimatedBuilder bọc MaterialApp để đổi theme mượt khi shop đổi (docs/60-design.md
    // "lerp() phải implement để animation đổi theme mượt") — KHÔNG làm mất Navigator vì
    // Flutter chỉ rebuild MaterialApp tại đúng vị trí trong cây, State của Navigator con
    // không bị huỷ.
    return AnimatedBuilder(
      animation: storefront,
      builder: (context, _) {
        final theme = DsTokens.fromThemeJson((storefront.layout ?? const {}).obj('theme'));
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          title: 'DynamicShop',
          theme: ThemeData(
            useMaterial3: true,
            colorScheme: ColorScheme.fromSeed(seedColor: theme.primary),
            scaffoldBackgroundColor: theme.surface,
            extensions: [theme],
          ),
          home: HomeScreen(
            storefrontController: storefront,
            cartController: cart,
            orderRepository: _orderRepository,
            onSwitchTenant: AppConfig.instance.isDev ? _switchTenant : null,
          ),
        );
      },
    );
  }
}

class _BootSplash extends StatelessWidget {
  const _BootSplash();

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(body: Center(child: CircularProgressIndicator())),
    );
  }
}

void unawaited(Future<void> future) {}
