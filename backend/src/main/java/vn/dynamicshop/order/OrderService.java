package vn.dynamicshop.order;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.dynamicshop.catalog.Product;
import vn.dynamicshop.catalog.ProductRepository;
import vn.dynamicshop.common.error.ApiException;
import vn.dynamicshop.order.dto.CreateOrderItemRequest;
import vn.dynamicshop.order.dto.CreateOrderRequest;
import vn.dynamicshop.order.dto.OrderResponseDto;
import vn.dynamicshop.outbox.OutboxPublisher;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderStateMachine orderStateMachine;
    private final OutboxPublisher outboxPublisher;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
            OrderStateMachine orderStateMachine, OutboxPublisher outboxPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderStateMachine = orderStateMachine;
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * Enqueue outbox TRONG CÙNG transaction tạo đơn — bất biến #7. FCM/gửi thật xảy ra
     * sau, ngoài transaction, ở {@code OutboxWorker}.
     */
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequest request) {
        Order order = new Order(generateOrderCode(), null, request.note(), request.deliveryAddress(),
                request.phone());

        for (CreateOrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .filter(p -> p.getDeletedAt() == null)
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));
            // SNAPSHOT name + price lúc đặt — bất biến #4, không join lại products sau này.
            order.addItem(new OrderItem(product.getId(), product.getName(), product.getPrice(),
                    itemRequest.qty(), itemRequest.options()));
        }
        order.recalculateTotals(0L, 0L);
        order = orderRepository.save(order);

        orderStateMachine.recordCreation(order, OrderEvent.ActorType.CUSTOMER, order.getUserId());

        outboxPublisher.enqueue(order.getId(), "NEW_ORDER", outboxPayload(order));

        return OrderResponseDto.from(order);
    }

    @Transactional
    public OrderResponseDto transitionStatus(UUID orderId, OrderStatus to, OrderEvent.ActorType actorType,
            UUID actorId, String reason) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        orderStateMachine.transitionOrderStatus(order, to, actorType, actorId, reason);
        outboxPublisher.enqueue(order.getId(), "ORDER_STATUS_CHANGED", outboxPayload(order));
        return OrderResponseDto.from(order);
    }

    private Map<String, Object> outboxPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId().toString());
        payload.put("code", order.getCode());
        payload.put("orderStatus", order.getOrderStatus().name());
        payload.put("total", order.getTotal());
        payload.put("at", Instant.now().toString());
        return payload;
    }

    /** Mã ngắn cho khách — unique per tenant qua constraint DB, không cần bảo đảm tuyệt đối ở đây. */
    private String generateOrderCode() {
        return "OD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static class ProductNotFoundException extends ApiException {
        public ProductNotFoundException(UUID productId) {
            super(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_FOUND", "Không tìm thấy món: " + productId);
        }
    }

    public static class OrderNotFoundException extends ApiException {
        public OrderNotFoundException(UUID orderId) {
            super(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn: " + orderId);
        }
    }
}
