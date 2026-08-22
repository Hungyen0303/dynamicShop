package vn.dynamicshop.order;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Đếm số dòng món cho cả một trang đơn bằng MỘT query, thay vì để mỗi
     * {@code order.getItems().size()} tự bắn một câu SELECT (N+1 trên endpoint nóng nhất).
     *
     * JPQL chứ không native SQL — Hibernate {@code @TenantId} chỉ tự thêm điều kiện tenant
     * cho JPQL/Criteria; native query sẽ tuột khỏi tầng đó và chỉ còn RLS đỡ (skill
     * tenant-isolation: "Không nativeQuery").
     */
    @Query("""
            select oi.order.id as orderId, count(oi) as cnt
            from OrderItem oi
            where oi.order.id in :orderIds
            group by oi.order.id
            """)
    @Transactional(readOnly = true)
    List<OrderItemCount> countByOrderIds(@Param("orderIds") Collection<UUID> orderIds);

    interface OrderItemCount {
        UUID getOrderId();

        long getCnt();
    }
}
