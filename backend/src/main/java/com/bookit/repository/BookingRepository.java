package com.bookit.repository;

import com.bookit.domain.Booking;
import com.bookit.domain.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** CONFIRMED reservations of a restaurant that overlap [from, to). */
    @Query("""
            select new com.bookit.repository.BusySlot(b.diningTable.id, b.startsAt, b.endsAt)
            from Booking b
            where b.restaurant.id = :restaurantId
              and b.status = com.bookit.domain.BookingStatus.CONFIRMED
              and b.startsAt < :to and b.endsAt > :from
            """)
    List<BusySlot> findBusy(@Param("restaurantId") Long restaurantId,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Does this customer already hold a confirmed booking (anywhere) overlapping the window? */
    @Query("""
            select count(b) > 0 from Booking b
            where b.user.id = :userId
              and b.status = com.bookit.domain.BookingStatus.CONFIRMED
              and b.startsAt < :to and b.endsAt > :from
            """)
    boolean existsOverlapForUser(@Param("userId") Long userId,
                                 @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select b from Booking b
            join fetch b.restaurant join fetch b.diningTable join fetch b.user
            where b.user.id = :userId
            order by b.startsAt desc
            """)
    List<Booking> findByUser(@Param("userId") Long userId);

    @Query(value = """
            select b from Booking b
            join fetch b.restaurant join fetch b.diningTable join fetch b.user
            """,
            countQuery = "select count(b) from Booking b")
    Page<Booking> findAllForAdmin(Pageable pageable);

    @Query(value = """
            select b from Booking b
            join fetch b.restaurant join fetch b.diningTable join fetch b.user
            where b.restaurant.id = :restaurantId
            """,
            countQuery = "select count(b) from Booking b where b.restaurant.id = :restaurantId")
    Page<Booking> findByRestaurantForAdmin(@Param("restaurantId") Long restaurantId, Pageable pageable);

    @Query("select count(b) from Booking b where b.status = :status and b.startsAt >= :from and b.startsAt < :to")
    long countByStatusBetween(@Param("status") BookingStatus status,
                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(b) from Booking b where b.status = :status and b.startsAt >= :from")
    long countByStatusFrom(@Param("status") BookingStatus status, @Param("from") LocalDateTime from);
}
