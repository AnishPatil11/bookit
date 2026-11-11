package com.bookit.dto;

import com.bookit.domain.Booking;
import com.bookit.domain.BookingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class BookingDtos {
    private BookingDtos() {}

    public record CreateBookingRequest(
            @NotNull Long restaurantId,
            @NotNull LocalDateTime startsAt,
            @Min(1) @Max(20) int partySize,
            @Size(max = 300) String notes) {}

    public record BookingStatusRequest(@NotNull BookingStatus status) {}

    public record BookingResponse(Long id, Long restaurantId, String restaurantName, String tableLabel,
                                  String customerName, String customerEmail, LocalDateTime startsAt,
                                  LocalDateTime endsAt, int partySize, BookingStatus status, String notes,
                                  boolean canCancel) {
        public static BookingResponse from(Booking b, boolean canCancel) {
            return new BookingResponse(b.getId(), b.getRestaurant().getId(), b.getRestaurant().getName(),
                    b.getDiningTable().getLabel(), b.getUser().getName(), b.getUser().getEmail(),
                    b.getStartsAt(), b.getEndsAt(), b.getPartySize(), b.getStatus(), b.getNotes(), canCancel);
        }
    }
}
