package com.bookit.dto;

import com.bookit.domain.DiningTable;
import com.bookit.domain.MenuItem;
import com.bookit.domain.Restaurant;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class RestaurantDtos {
    private RestaurantDtos() {}

    public record RestaurantResponse(Long id, String name, String description, String cuisine, String address,
                                     String city, LocalTime openTime, LocalTime closeTime, String imageUrl,
                                     boolean active) {
        public static RestaurantResponse from(Restaurant r) {
            return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getCuisine(), r.getAddress(),
                    r.getCity(), r.getOpenTime(), r.getCloseTime(), r.getImageUrl(), r.isActive());
        }
    }

    public record MenuItemResponse(Long id, String name, String description, String category, BigDecimal price,
                                   boolean available) {
        public static MenuItemResponse from(MenuItem m) {
            return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getCategory(), m.getPrice(),
                    m.isAvailable());
        }
    }

    public record TableResponse(Long id, String label, int capacity, boolean active) {
        public static TableResponse from(DiningTable t) {
            return new TableResponse(t.getId(), t.getLabel(), t.getCapacity(), t.isActive());
        }
    }

    public record SlotResponse(LocalDateTime startsAt, LocalDateTime endsAt, boolean available) {}

    public record AvailabilityResponse(LocalDate date, int partySize, List<SlotResponse> slots) {}

    // ----- admin write models -----

    public record RestaurantRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 60) String cuisine,
            @Size(max = 200) String address,
            @NotBlank @Size(max = 80) String city,
            @NotNull LocalTime openTime,
            @NotNull LocalTime closeTime,
            @Size(max = 500) String imageUrl,
            Boolean active) {}

    public record TableRequest(
            @NotBlank @Size(max = 40) String label,
            @Min(1) @Max(20) int capacity,
            Boolean active) {}

    public record MenuItemRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @DecimalMax(value = "99999.99") BigDecimal price,
            Boolean available) {}
}
