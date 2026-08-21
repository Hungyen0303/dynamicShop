package vn.dynamicshop.storefront;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontRepository extends JpaRepository<Storefront, UUID> {
}
