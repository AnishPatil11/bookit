package com.bookit.web;

import com.bookit.dto.BookingDtos.BookingResponse;
import com.bookit.dto.BookingDtos.CreateBookingRequest;
import com.bookit.security.AuthPrincipal;
import com.bookit.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                  @Valid @RequestBody CreateBookingRequest req) {
        return service.create(principal, req);
    }

    @GetMapping("/my")
    public List<BookingResponse> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.myBookings(principal);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        return service.cancel(principal, id);
    }
}
