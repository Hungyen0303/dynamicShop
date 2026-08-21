package vn.dynamicshop.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCategory_IdAndDeletedAtIsNull(UUID categoryId);

    List<Product> findByDeletedAtIsNull();
}
