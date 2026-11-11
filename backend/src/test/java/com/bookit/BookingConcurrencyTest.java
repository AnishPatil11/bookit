package com.bookit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bookit.domain.DiningTable;
import com.bookit.domain.Restaurant;
import com.bookit.domain.Role;
import com.bookit.domain.User;
import com.bookit.dto.BookingDtos.CreateBookingRequest;
import com.bookit.exception.ApiException;
import com.bookit.repository.DiningTableRepository;
import com.bookit.repository.RestaurantRepository;
import com.bookit.repository.UserRepository;
import com.bookit.security.AuthPrincipal;
import com.bookit.service.BookingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Eight customers race for the only table at the same moment: exactly one may win. */
class BookingConcurrencyTest extends IntegrationTestBase {

    @Autowired BookingService bookingService;
    @Autowired UserRepository users;
    @Autowired RestaurantRepository restaurants;
    @Autowired DiningTableRepository tables;

    @Test
    void onlyOneOfManyConcurrentBookingsSucceeds() throws Exception {
        Restaurant restaurant = restaurants.save(new Restaurant("Race Cafe " + UUID.randomUUID(), "d", "Cafe",
                "addr", "Hubballi", LocalTime.of(10, 0), LocalTime.of(23, 0), null, LocalDateTime.now(ZONE)));
        tables.save(new DiningTable(restaurant, "ONLY", 4));
        LocalDateTime slot = LocalDate.now(ZONE).plusDays(2).atTime(19, 0);

        int contenders = 8;
        List<AuthPrincipal> principals = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            User u = users.save(new User("Racer " + i, "racer-" + UUID.randomUUID() + "@test.dev", "x",
                    Role.CUSTOMER, LocalDateTime.now(ZONE)));
            principals.add(new AuthPrincipal(u.getId(), u.getEmail(), u.getRole()));
        }

        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (AuthPrincipal p : principals) {
            Callable<String> task = () -> {
                gate.await();
                try {
                    bookingService.create(p, new CreateBookingRequest(restaurant.getId(), slot, 2, null));
                    return "OK";
                } catch (ApiException ex) {
                    return "CONFLICT:" + ex.getStatus().value();
                }
            };
            results.add(pool.submit(task));
        }
        gate.countDown();

        int ok = 0;
        int conflicts = 0;
        for (Future<String> f : results) {
            String outcome = f.get(60, TimeUnit.SECONDS);
            if (outcome.equals("OK")) {
                ok++;
            } else if (outcome.equals("CONFLICT:409")) {
                conflicts++;
            }
        }
        pool.shutdown();

        assertEquals(1, ok, "exactly one booking must win the table");
        assertEquals(contenders - 1, conflicts, "everyone else must get a clean 409");
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
    }
}
