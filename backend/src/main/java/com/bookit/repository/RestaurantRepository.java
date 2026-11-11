package com.bookit.repository;

import com.bookit.domain.Restaurant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** Empty strings mean "no filter" (avoids PostgreSQL's untyped-null problem with optional params). */
    @Query("""
            select r from Restaurant r
            where r.active = true
              and (lower(r.name) like lower(concat('%', :q, '%')) or lower(r.cuisine) like lower(concat('%', :q, '%')))
              and lower(r.city) like lower(concat('%', :city, '%'))
              and lower(r.cuisine) like lower(concat('%', :cuisine, '%'))
            """)
    Page<Restaurant> search(@Param("q") String q, @Param("city") String city,
                            @Param("cuisine") String cuisine, Pageable pageable);

    @Query("select distinct r.cuisine from Restaurant r where r.active = true order by r.cuisine")
    List<String> findActiveCuisines();

    /** SELECT ... FOR UPDATE on the restaurant row: serialises bookings per restaurant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Restaurant r where r.id = :id")
    Optional<Restaurant> findByIdForUpdate(@Param("id") Long id);
}
