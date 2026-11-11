package com.bookit.web;

import com.bookit.domain.OrderStatus;
import com.bookit.dto.BookingDtos.BookingResponse;
import com.bookit.dto.BookingDtos.BookingStatusRequest;
import com.bookit.dto.OrderDtos.OrderResponse;
import com.bookit.dto.OrderDtos.OrderStatusRequest;
import com.bookit.dto.PageResponse;
import com.bookit.dto.RestaurantDtos.MenuItemRequest;
import com.bookit.dto.RestaurantDtos.MenuItemResponse;
import com.bookit.dto.RestaurantDtos.RestaurantRequest;
import com.bookit.dto.RestaurantDtos.RestaurantResponse;
import com.bookit.dto.RestaurantDtos.TableRequest;
import com.bookit.dto.RestaurantDtos.TableResponse;
import com.bookit.dto.StatsResponse;
import com.bookit.service.BookingService;
import com.bookit.service.OrderService;
import com.bookit.service.RestaurantService;
import com.bookit.service.StatsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/admin requires the ADMIN role (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RestaurantService restaurants;
    private final BookingService bookings;
    private final OrderService orders;
    private final StatsService stats;

    public AdminController(RestaurantService restaurants, BookingService bookings, OrderService orders,
                           StatsService stats) {
        this.restaurants = restaurants;
        this.bookings = bookings;
        this.orders = orders;
        this.stats = stats;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return stats.stats();
    }

    // ---- restaurants
    @GetMapping("/restaurants")
    public List<RestaurantResponse> restaurants() {
        return restaurants.adminList();
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantRequest req) {
        return restaurants.create(req);
    }

    @PutMapping("/restaurants/{id}")
    public RestaurantResponse updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantRequest req) {
        return restaurants.update(id, req);
    }

    // ---- tables
    @GetMapping("/restaurants/{id}/tables")
    public List<TableResponse> tables(@PathVariable Long id) {
        return restaurants.adminTables(id);
    }

    @PostMapping("/restaurants/{id}/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse addTable(@PathVariable Long id, @Valid @RequestBody TableRequest req) {
        return restaurants.addTable(id, req);
    }

    @PutMapping("/restaurants/{id}/tables/{tableId}")
    public TableResponse updateTable(@PathVariable Long id, @PathVariable Long tableId,
                                     @Valid @RequestBody TableRequest req) {
        return restaurants.updateTable(id, tableId, req);
    }

    // ---- menu
    @GetMapping("/restaurants/{id}/menu-items")
    public List<MenuItemResponse> menu(@PathVariable Long id) {
        return restaurants.adminMenu(id);
    }

    @PostMapping("/restaurants/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse addMenuItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest req) {
        return restaurants.addMenuItem(id, req);
    }

    @PutMapping("/restaurants/{id}/menu-items/{itemId}")
    public MenuItemResponse updateMenuItem(@PathVariable Long id, @PathVariable Long itemId,
                                           @Valid @RequestBody MenuItemRequest req) {
        return restaurants.updateMenuItem(id, itemId, req);
    }

    @DeleteMapping("/restaurants/{id}/menu-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(@PathVariable Long id, @PathVariable Long itemId) {
        restaurants.deleteMenuItem(id, itemId);
    }

    // ---- bookings
    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(@RequestParam(required = false) Long restaurantId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return bookings.adminList(restaurantId, page, size);
    }

    @PostMapping("/bookings/{id}/status")
    public BookingResponse setBookingStatus(@PathVariable Long id, @Valid @RequestBody BookingStatusRequest req) {
        return bookings.adminSetStatus(id, req.status());
    }

    // ---- orders
    @GetMapping("/orders")
    public PageResponse<OrderResponse> orders(@RequestParam(required = false) OrderStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return orders.adminList(status, page, size);
    }

    @PostMapping("/orders/{id}/status")
    public OrderResponse setOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest req) {
        return orders.adminSetStatus(id, req.status());
    }
}
