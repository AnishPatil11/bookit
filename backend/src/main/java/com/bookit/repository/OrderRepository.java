package com.bookit.repository;

import com.bookit.domain.CustomerOrder;
import com.bookit.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query("""
            select o from CustomerOrder o
            join fetch o.restaurant join fetch o.user
            where o.user.id = :userId
            order by o.createdAt desc
            """)
    List<CustomerOrder> findByUser(@Param("userId") Long userId);

    @Query(value = "select o from CustomerOrder o join fetch o.restaurant join fetch o.user",
            countQuery = "select count(o) from CustomerOrder o")
    Page<CustomerOrder> findAllForAdmin(Pageable pageable);

    @Query(value = "select o from CustomerOrder o join fetch o.restaurant join fetch o.user where o.status = :status",
            countQuery = "select count(o) from CustomerOrder o where o.status = :status")
    Page<CustomerOrder> findByStatusForAdmin(@Param("status") OrderStatus status, Pageable pageable);

    @Query("select count(o) from CustomerOrder o where o.createdAt >= :from and o.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from CustomerOrder o where o.status in :statuses")
    long countByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    /** Revenue of every order that was not cancelled. Returns null when there are none. */
    @Query("select sum(o.total) from CustomerOrder o where o.status <> com.bookit.domain.OrderStatus.CANCELLED")
    BigDecimal sumRevenue();
}
