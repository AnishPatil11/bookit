package com.bookit.dto;

import java.math.BigDecimal;

public record StatsResponse(long restaurants, long bookingsToday, long upcomingBookings, long ordersToday,
                            long openOrders, BigDecimal revenue) {}
