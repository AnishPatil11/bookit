package com.bookit.service;

import com.bookit.config.AppProperties;
import com.bookit.domain.DiningTable;
import com.bookit.domain.MenuItem;
import com.bookit.domain.Restaurant;
import com.bookit.dto.PageResponse;
import com.bookit.dto.RestaurantDtos.AvailabilityResponse;
import com.bookit.dto.RestaurantDtos.MenuItemRequest;
import com.bookit.dto.RestaurantDtos.MenuItemResponse;
import com.bookit.dto.RestaurantDtos.RestaurantRequest;
import com.bookit.dto.RestaurantDtos.RestaurantResponse;
import com.bookit.dto.RestaurantDtos.SlotResponse;
import com.bookit.dto.RestaurantDtos.TableRequest;
import com.bookit.dto.RestaurantDtos.TableResponse;
import com.bookit.exception.ApiException;
import com.bookit.repository.BookingRepository;
import com.bookit.repository.BusySlot;
import com.bookit.repository.DiningTableRepository;
import com.bookit.repository.MenuItemRepository;
import com.bookit.repository.RestaurantRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final MenuItemRepository menuItems;
    private final BookingRepository bookings;
    private final AppProperties props;
    private final Clock clock;

    public RestaurantService(RestaurantRepository restaurants, DiningTableRepository tables,
                             MenuItemRepository menuItems, BookingRepository bookings,
                             AppProperties props, Clock clock) {
        this.restaurants = restaurants;
        this.tables = tables;
        this.menuItems = menuItems;
        this.bookings = bookings;
        this.props = props;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- public reads

    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> search(String q, String city, String cuisine, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("name"));
        Page<Restaurant> result = restaurants.search(blank(q), blank(city), blank(cuisine), pageable);
        return PageResponse.from(result.map(RestaurantResponse::from));
    }

    @Transactional(readOnly = true)
    public List<String> cuisines() {
        return restaurants.findActiveCuisines();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(Long id) {
        return RestaurantResponse.from(activeRestaurant(id));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> menu(Long id) {
        activeRestaurant(id);
        return menuItems.findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(id)
                .stream().map(MenuItemResponse::from).toList();
    }

    /** Slot grid for one day: each slot is free if at least one big-enough table has no overlapping booking. */
    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long id, LocalDate date, int partySize) {
        Restaurant r = activeRestaurant(id);
        if (partySize < 1 || partySize > 20) {
            throw ApiException.badRequest("Party size must be between 1 and 20");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (date.isBefore(now.toLocalDate()) || date.isAfter(now.toLocalDate().plusDays(props.reservation().maxDaysAhead()))) {
            throw ApiException.badRequest("Date must be within the next " + props.reservation().maxDaysAhead() + " days");
        }

        List<DiningTable> fitting = tables.findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(id).stream()
                .filter(t -> t.getCapacity() >= partySize).toList();
        List<BusySlot> busy = bookings.findBusy(id, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        Duration length = Duration.ofMinutes(props.reservation().slotMinutes());
        Duration step = Duration.ofMinutes(props.reservation().slotStepMinutes());
        LocalDateTime closes = date.atTime(r.getCloseTime());

        List<SlotResponse> slots = new ArrayList<>();
        for (LocalDateTime s = date.atTime(r.getOpenTime()); !s.plus(length).isAfter(closes); s = s.plus(step)) {
            if (s.isBefore(now)) {
                continue;
            }
            final LocalDateTime start = s;
            final LocalDateTime end = s.plus(length);
            boolean free = fitting.stream().anyMatch(t -> busy.stream()
                    .noneMatch(b -> b.tableId().equals(t.getId()) && b.overlaps(start, end)));
            slots.add(new SlotResponse(start, end, free));
        }
        return new AvailabilityResponse(date, partySize, slots);
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<RestaurantResponse> adminList() {
        return restaurants.findAll(Sort.by("name")).stream().map(RestaurantResponse::from).toList();
    }

    @Transactional
    public RestaurantResponse create(RestaurantRequest req) {
        checkHours(req);
        Restaurant r = new Restaurant(req.name().trim(), req.description(), req.cuisine().trim(), req.address(),
                req.city().trim(), req.openTime(), req.closeTime(), req.imageUrl(), LocalDateTime.now(clock));
        return RestaurantResponse.from(restaurants.save(r));
    }

    @Transactional
    public RestaurantResponse update(Long id, RestaurantRequest req) {
        checkHours(req);
        Restaurant r = restaurants.findById(id).orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        r.update(req.name().trim(), req.description(), req.cuisine().trim(), req.address(), req.city().trim(),
                req.openTime(), req.closeTime(), req.imageUrl(), req.active() == null || req.active());
        return RestaurantResponse.from(r);
    }

    @Transactional(readOnly = true)
    public List<TableResponse> adminTables(Long restaurantId) {
        anyRestaurant(restaurantId);
        return tables.findByRestaurantIdOrderByLabelAsc(restaurantId).stream().map(TableResponse::from).toList();
    }

    @Transactional
    public TableResponse addTable(Long restaurantId, TableRequest req) {
        Restaurant r = anyRestaurant(restaurantId);
        if (tables.existsByRestaurantIdAndLabelIgnoreCase(restaurantId, req.label().trim())) {
            throw ApiException.conflict("A table with this label already exists");
        }
        try {
            DiningTable table = new DiningTable(r, req.label().trim(), req.capacity());
            table.update(req.label().trim(), req.capacity(), req.active() == null || req.active());
            return TableResponse.from(tables.saveAndFlush(table));
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("A table with this label already exists");
        }
    }

    @Transactional
    public TableResponse updateTable(Long restaurantId, Long tableId, TableRequest req) {
        DiningTable t = tables.findById(tableId)
                .filter(x -> x.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> ApiException.notFound("Table not found"));
        t.update(req.label().trim(), req.capacity(), req.active() == null || req.active());
        return TableResponse.from(t);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> adminMenu(Long restaurantId) {
        anyRestaurant(restaurantId);
        return menuItems.findByRestaurantIdOrderByCategoryAscNameAsc(restaurantId)
                .stream().map(MenuItemResponse::from).toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long restaurantId, MenuItemRequest req) {
        Restaurant r = anyRestaurant(restaurantId);
        MenuItem m = new MenuItem(r, req.name().trim(), req.description(), req.category().trim(), req.price());
        m.update(req.name().trim(), req.description(), req.category().trim(), req.price(),
        req.available() == null || req.available());
        return MenuItemResponse.from(menuItems.save(m));
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long restaurantId, Long itemId, MenuItemRequest req) {
        MenuItem m = menuItems.findById(itemId)
                .filter(x -> x.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> ApiException.notFound("Menu item not found"));
        m.update(req.name().trim(), req.description(), req.category().trim(), req.price(),
                req.available() == null || req.available());
        return MenuItemResponse.from(m);
    }

    @Transactional
    public void deleteMenuItem(Long restaurantId, Long itemId) {
        MenuItem m = menuItems.findById(itemId)
                .filter(x -> x.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> ApiException.notFound("Menu item not found"));
        menuItems.delete(m);
    }

    // ---------------------------------------------------------------- helpers

    private Restaurant activeRestaurant(Long id) {
        return restaurants.findById(id).filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
    }

    private Restaurant anyRestaurant(Long id) {
        return restaurants.findById(id).orElseThrow(() -> ApiException.notFound("Restaurant not found"));
    }

    private static void checkHours(RestaurantRequest req) {
        if (!req.closeTime().isAfter(req.openTime())) {
            throw ApiException.badRequest("Closing time must be after opening time");
        }
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}
