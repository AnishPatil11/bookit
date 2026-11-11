package com.bookit.service;

import com.bookit.config.AppProperties;
import com.bookit.domain.Booking;
import com.bookit.domain.BookingStatus;
import com.bookit.domain.DiningTable;
import com.bookit.domain.Restaurant;
import com.bookit.domain.User;
import com.bookit.dto.BookingDtos.BookingResponse;
import com.bookit.dto.BookingDtos.CreateBookingRequest;
import com.bookit.dto.PageResponse;
import com.bookit.exception.ApiException;
import com.bookit.repository.BookingRepository;
import com.bookit.repository.BusySlot;
import com.bookit.repository.DiningTableRepository;
import com.bookit.repository.RestaurantRepository;
import com.bookit.repository.UserRepository;
import com.bookit.security.AuthPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The booking engine.
 *
 * Double-booking is prevented in three layers:
 *  1. SELECT ... FOR UPDATE on the restaurant row, so concurrent bookings for one restaurant run one at a time.
 *  2. An overlap check against CONFIRMED bookings, then best-fit table choice (smallest table that fits).
 *  3. On PostgreSQL, an exclusion constraint (V2 migration) that rejects overlapping CONFIRMED rows outright.
 */
@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final UserRepository users;
    private final AppProperties props;
    private final Clock clock;

    public BookingService(BookingRepository bookings, RestaurantRepository restaurants, DiningTableRepository tables,
                          UserRepository users, AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.restaurants = restaurants;
        this.tables = tables;
        this.users = users;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(AuthPrincipal principal, CreateBookingRequest req) {
        LocalDateTime now = LocalDateTime.now(clock);

        Restaurant restaurant = restaurants.findByIdForUpdate(req.restaurantId())
                .filter(Restaurant::isActive)
                .orElseThrow(() -> ApiException.notFound("Restaurant not found"));

        LocalDateTime start = req.startsAt();
        LocalDateTime end = start.plusMinutes(props.reservation().slotMinutes());
        validateSlot(restaurant, start, end, now);

        if (bookings.existsOverlapForUser(principal.id(), start, end)) {
            throw ApiException.conflict("You already have a booking that overlaps this time");
        }

        List<BusySlot> busy = bookings.findBusy(restaurant.getId(), start, end);
        DiningTable table = tables.findByRestaurantIdAndActiveTrueOrderByCapacityAscLabelAsc(restaurant.getId()).stream()
                .filter(t -> t.getCapacity() >= req.partySize())
                .filter(t -> busy.stream().noneMatch(b -> b.tableId().equals(t.getId())))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("No table is free for that time and party size"));

        User user = users.findById(principal.id())
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        Booking booking = new Booking(user, restaurant, table, start, end, req.partySize(), req.notes(), now);
        try {
            bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            // The PostgreSQL exclusion constraint fired: someone else got the table first.
            throw ApiException.conflict("That table was just taken. Please pick another time.");
        }
        return BookingResponse.from(booking, canCancel(booking, now));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(AuthPrincipal principal) {
        LocalDateTime now = LocalDateTime.now(clock);
        return bookings.findByUser(principal.id()).stream()
                .map(b -> BookingResponse.from(b, canCancel(b, now))).toList();
    }

    @Transactional
    public BookingResponse cancel(AuthPrincipal principal, Long id) {
        LocalDateTime now = LocalDateTime.now(clock);
        Booking b = bookings.findById(id)
                .filter(x -> principal.isAdmin() || x.getUser().getId().equals(principal.id()))
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.badRequest("Only confirmed bookings can be cancelled");
        }
        if (!principal.isAdmin() && !canCancel(b, now)) {
            throw ApiException.badRequest("Bookings can only be cancelled up to "
                    + props.reservation().cancelCutoffHours() + " hours before they start");
        }
        b.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(b, false);
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> adminList(Long restaurantId, int page, int size) {
        LocalDateTime now = LocalDateTime.now(clock);
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startsAt"));
        var result = restaurantId == null
                ? bookings.findAllForAdmin(pageable)
                : bookings.findByRestaurantForAdmin(restaurantId, pageable);
        return PageResponse.from(result.map(b -> BookingResponse.from(b, canCancel(b, now))));
    }

    @Transactional
    public BookingResponse adminSetStatus(Long id, BookingStatus target) {
        LocalDateTime now = LocalDateTime.now(clock);
        Booking b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (b.getStatus() != BookingStatus.CONFIRMED || target == BookingStatus.CONFIRMED) {
            throw ApiException.badRequest("A confirmed booking can be marked completed, no-show or cancelled");
        }
        b.setStatus(target);
        return BookingResponse.from(b, canCancel(b, now));
    }

    // ------------------------------------------------------------------ rules

    private void validateSlot(Restaurant r, LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (start.getSecond() != 0 || start.getNano() != 0) {
            throw ApiException.badRequest("Start time must be on a whole minute");
        }
        if (start.isBefore(now)) {
            throw ApiException.badRequest("Start time is in the past");
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(props.reservation().maxDaysAhead()))) {
            throw ApiException.badRequest("Bookings open " + props.reservation().maxDaysAhead() + " days ahead");
        }
        LocalTime open = r.getOpenTime();
        boolean sameDay = end.toLocalDate().equals(start.toLocalDate());
        if (start.toLocalTime().isBefore(open) || !sameDay || end.toLocalTime().isAfter(r.getCloseTime())) {
            throw ApiException.badRequest("The restaurant is not open for the whole booking window");
        }
        long minutesFromOpen = Duration.between(open, start.toLocalTime()).toMinutes();
        if (minutesFromOpen % props.reservation().slotStepMinutes() != 0) {
            throw ApiException.badRequest("Start time must be one of the available slots");
        }
    }

    private boolean canCancel(Booking b, LocalDateTime now) {
        return b.getStatus() == BookingStatus.CONFIRMED
                && !now.plusHours(props.reservation().cancelCutoffHours()).isAfter(b.getStartsAt());
    }
}
