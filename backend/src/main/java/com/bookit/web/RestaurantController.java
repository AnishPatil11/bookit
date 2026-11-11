package com.bookit.web;

import com.bookit.dto.PageResponse;
import com.bookit.dto.RestaurantDtos.AvailabilityResponse;
import com.bookit.dto.RestaurantDtos.MenuItemResponse;
import com.bookit.dto.RestaurantDtos.RestaurantResponse;
import com.bookit.service.RestaurantService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only browsing. */
@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<RestaurantResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String cuisine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.search(q, city, cuisine, page, size);
    }

    @GetMapping("/cuisines")
    public List<String> cuisines() {
        return service.cuisines();
    }

    @GetMapping("/{id}")
    public RestaurantResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/menu")
    public List<MenuItemResponse> menu(@PathVariable Long id) {
        return service.menu(id);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "2") int partySize) {
        return service.availability(id, date, partySize);
    }
}
