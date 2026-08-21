package vn.dynamicshop.outbox;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByProcessedAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
