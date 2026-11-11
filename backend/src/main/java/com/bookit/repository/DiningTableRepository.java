package com.bookit.repository;

import com.bookit.domain.DiningTable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(Long restaurantId);
    List<DiningTable> findByRestaurantIdOrderByLabelAsc(Long restaurantId);
    boolean existsByRestaurantIdAndLabelIgnoreCase(Long restaurantId, String label);
}
