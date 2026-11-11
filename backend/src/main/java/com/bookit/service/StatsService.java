package com.bookit.service;

import com.bookit.domain.BookingStatus;
import com.bookit.domain.OrderStatus;
import com.bookit.dto.StatsResponse;
import com.bookit.repository.BookingRepository;
import com.bookit.repository.OrderRepository;
import com.bookit.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final RestaurantRepository restaurants;
    private final BookingRepository bookings;
    private final OrderRepository orders;
    private final Clock clock;

    public StatsService(RestaurantRepository restaurants, BookingRepository bookings, OrderRepository orders, Clock clock) {
        this.restaurants = restaurants;
        this.bookings = bookings;
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime dayStart = LocalDate.now(clock).atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        BigDecimal revenue = orders.sumRevenue();
        return new StatsResponse(
                restaurants.count(),
                bookings.countByStatusBetween(BookingStatus.CONFIRMED, dayStart, dayEnd),
                bookings.countByStatusFrom(BookingStatus.CONFIRMED, now),
                orders.countCreatedBetween(dayStart, dayEnd),
                orders.countByStatusIn(List.of(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.READY)),
                revenue == null ? BigDecimal.ZERO : revenue);
    }
}
