package ru.yandex.practicum.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.inventory.entity.InventoryUnit;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryUnit, Long> {
    boolean existsByProductId(long productId);

    Optional<InventoryUnit> findByProductId(long productId);

}
