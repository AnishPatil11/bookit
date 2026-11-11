package com.bookit.service;

import com.bookit.domain.Booking;
import com.bookit.domain.BookingStatus;
import com.bookit.domain.CustomerOrder;
import com.bookit.domain.MenuItem;
import com.bookit.domain.OrderItem;
import com.bookit.domain.OrderStatus;
import com.bookit.domain.Restaurant;
import com.bookit.domain.User;
import com.bookit.dto.OrderDtos.CreateOrderRequest;
import com.bookit.dto.OrderDtos.Line;
import com.bookit.dto.OrderDtos.OrderResponse;
import com.bookit.dto.PageResponse;
import com.bookit.exception.ApiException;
import com.bookit.repository.BookingRepository;
import com.bookit.repository.MenuItemRepository;
import com.bookit.repository.OrderRepository;
import com.bookit.repository.RestaurantRepository;
import com.bookit.repository.UserRepository;
import com.bookit.security.AuthPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final Clock clock;

    public OrderService(OrderRepository orders, RestaurantRepository restaurants, MenuItemRepository menuItems,
                        BookingRepository bookings, UserRepository users, Clock clock) {
        this.orders = orders;
        this.restaurants = restaurants;
        this.menuItems = menuItems;
        this.bookings = bookings;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public OrderResponse create(AuthPrincipal principal, CreateOrderRequest req) {
        Restaurant restaurant = restaurants.findById(req.restaurantId())
                .filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        User user = users.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));

        Booking booking = null;
        if (req.bookingId() != null) {
            booking = bookings.findById(req.bookingId())
                    .orElseThrow(() -> ApiException.notFound("Booking not found"));
            boolean mine = booking.getUser().getId().equals(principal.id())
                    && booking.getRestaurant().getId().equals(restaurant.getId());
            if (!mine) {
                throw ApiException.badRequest("That booking does not belong to you at this restaurant");
            }
            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw ApiException.badRequest("Orders can only be attached to a confirmed booking");
            }
        }

        // Merge repeated lines so the same dish is stored once with the summed quantity.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (Line line : req.items()) {
            quantities.merge(line.menuItemId(), line.quantity(), Integer::sum);
        }
        Map<Long, MenuItem> found = menuItems.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        if (found.size() != quantities.size()) {
            throw ApiException.badRequest("One or more menu items do not exist");
        }

        CustomerOrder order = new CustomerOrder(user, restaurant, booking, req.notes(), LocalDateTime.now(clock));
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            MenuItem item = found.get(entry.getKey());
            if (!item.getRestaurant().getId().equals(restaurant.getId()) || !item.isAvailable()) {
                throw ApiException.badRequest("\"" + item.getName() + "\" is not available at this restaurant");
            }
            if (entry.getValue() > 50) {
                throw ApiException.badRequest("At most 50 of one item per order");
            }
            order.addItem(new OrderItem(order, item.getId(), item.getName(), item.getPrice(), entry.getValue()));
        }
        orders.save(order);
        return OrderResponse.from(order, canCancel(order, principal));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> myOrders(AuthPrincipal principal) {
        return orders.findByUser(principal.id()).stream()
                .map(o -> OrderResponse.from(o, canCancel(o, principal))).toList();
    }

    @Transactional
    public OrderResponse cancel(AuthPrincipal principal, Long id) {
        CustomerOrder o = orders.findById(id)
                .filter(x -> principal.isAdmin() || x.getUser().getId().equals(principal.id()))
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!canCancel(o, principal)) {
            throw ApiException.badRequest(principal.isAdmin()
                    ? "This order can no longer be cancelled"
                    : "Only orders that have not started preparing can be cancelled");
        }
        o.setStatus(OrderStatus.CANCELLED);
        return OrderResponse.from(o, false);
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> adminList(OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null ? orders.findAllForAdmin(pageable) : orders.findByStatusForAdmin(status, pageable);
        return PageResponse.from(result.map(o -> OrderResponse.from(o, o.getStatus().canTransitionTo(OrderStatus.CANCELLED))));
    }

    @Transactional
    public OrderResponse adminSetStatus(Long id, OrderStatus target) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!o.getStatus().canTransitionTo(target)) {
            throw ApiException.badRequest("Cannot move an order from " + o.getStatus() + " to " + target);
        }
        o.setStatus(target);
        return OrderResponse.from(o, o.getStatus().canTransitionTo(OrderStatus.CANCELLED));
    }

    private boolean canCancel(CustomerOrder o, AuthPrincipal principal) {
        return principal.isAdmin()
                ? o.getStatus().canTransitionTo(OrderStatus.CANCELLED)
                : o.getStatus() == OrderStatus.PLACED;
    }
}
