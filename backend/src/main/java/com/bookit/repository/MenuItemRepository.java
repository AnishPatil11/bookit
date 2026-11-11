package com.bookit.repository;

import com.bookit.domain.MenuItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(Long restaurantId);
    List<MenuItem> findByRestaurantIdOrderByCategoryAscNameAsc(Long restaurantId);
}
