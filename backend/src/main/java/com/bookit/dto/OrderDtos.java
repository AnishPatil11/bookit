package com.bookit.dto;

import com.bookit.domain.CustomerOrder;
import com.bookit.domain.OrderItem;
import com.bookit.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record Line(@NotNull Long menuItemId, @Min(1) @Max(50) int quantity) {}

    public record CreateOrderRequest(
            @NotNull Long restaurantId,
            Long bookingId,
            @NotEmpty @Valid List<Line> items,
            @Size(max = 300) String notes) {}

    public record OrderStatusRequest(@NotNull OrderStatus status) {}

    public record OrderItemResponse(String name, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        public static OrderItemResponse from(OrderItem i) {
            return new OrderItemResponse(i.getName(), i.getUnitPrice(), i.getQuantity(), i.lineTotal());
        }
    }

    public record OrderResponse(Long id, Long restaurantId, String restaurantName, Long bookingId,
                                String customerName, OrderStatus status, BigDecimal total, String notes,
                                LocalDateTime createdAt, List<OrderItemResponse> items, boolean canCancel) {
        public static OrderResponse from(CustomerOrder o, boolean canCancel) {
            return new OrderResponse(o.getId(), o.getRestaurant().getId(), o.getRestaurant().getName(),
                    o.getBooking() == null ? null : o.getBooking().getId(), o.getUser().getName(), o.getStatus(),
                    o.getTotal(), o.getNotes(), o.getCreatedAt(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(), canCancel);
        }
    }
}
