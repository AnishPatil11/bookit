package com.bookit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class BookingFlowIntegrationTest extends IntegrationTestBase {

    private ResultActions book(String token, long restaurantId, LocalDateTime start, int party) throws Exception {
        return mvc.perform(post("/api/bookings").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(bookingBody(restaurantId, start, party))));
    }

    @Test
    void secondBookingForTheSameSingleTableIsRejected() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        LocalDateTime slot = tomorrowAt(19, 0);

        book(registerCustomer(), restaurant, slot, 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.tableLabel").value("T1"));

        book(registerCustomer(), restaurant, slot, 2).andExpect(status().isConflict());
        // Overlapping (starts 30 minutes later, still inside the 90-minute hold) is also rejected.
        book(registerCustomer(), restaurant, tomorrowAt(19, 30), 2).andExpect(status().isConflict());
        // Back-to-back (starts exactly when the first one ends) is fine.
        book(registerCustomer(), restaurant, tomorrowAt(20, 30), 2).andExpect(status().isCreated());
    }

    @Test
    void smallestFittingTableIsChosen() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "BIG", 8);
        addTable(admin, restaurant, "SMALL", 2);
        book(registerCustomer(), restaurant, tomorrowAt(18, 0), 2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableLabel").value("SMALL"));
        book(registerCustomer(), restaurant, tomorrowAt(18, 0), 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableLabel").value("BIG"));
    }

    @Test
    void partyLargerThanEveryTableIsRejected() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        book(registerCustomer(), restaurant, tomorrowAt(19, 0), 6).andExpect(status().isConflict());
    }

    @Test
    void availabilityReflectsBookingsAndCancellationFreesTheSlot() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        LocalDateTime slot = tomorrowAt(20, 0);
        String customer = registerCustomer();

        assertTrue(slotAvailable(restaurant, slot));
        MvcResult created = book(customer, restaurant, slot, 2).andExpect(status().isCreated()).andReturn();
        assertFalse(slotAvailable(restaurant, slot));

        long bookingId = read(created).get("id").asLong();
        mvc.perform(post("/api/bookings/" + bookingId + "/cancel").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertTrue(slotAvailable(restaurant, slot));
        // and somebody else can now take it
        book(registerCustomer(), restaurant, slot, 2).andExpect(status().isCreated());
    }

    @Test
    void invalidTimesAreRejected() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        String customer = registerCustomer();

        book(customer, restaurant, tomorrowAt(19, 15), 2).andExpect(status().isBadRequest());   // off the slot grid
        book(customer, restaurant, tomorrowAt(8, 0), 2).andExpect(status().isBadRequest());     // before opening
        book(customer, restaurant, tomorrowAt(22, 30), 2).andExpect(status().isBadRequest());   // would end after closing
        book(customer, restaurant, LocalDateTime.now(ZONE).minusDays(1).withMinute(0).withSecond(0).withNano(0), 2)
                .andExpect(status().isBadRequest());                                           // in the past
        book(customer, restaurant, tomorrowAt(19, 0), 0).andExpect(status().isBadRequest());   // bean validation
        book(customer, 999_999L, tomorrowAt(19, 0), 2).andExpect(status().isNotFound());        // unknown restaurant
    }

    @Test
    void sameCustomerCannotHoldTwoOverlappingBookings() throws Exception {
        String admin = adminToken();
        long first = createRestaurant(admin);
        long second = createRestaurant(admin);
        addTable(admin, first, "T1", 4);
        addTable(admin, second, "T1", 4);
        String customer = registerCustomer();

        book(customer, first, tomorrowAt(19, 0), 2).andExpect(status().isCreated());
        book(customer, second, tomorrowAt(19, 30), 2).andExpect(status().isConflict());
    }

    @Test
    void customersCannotCancelEachOthersBookings() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        MvcResult created = book(registerCustomer(), restaurant, tomorrowAt(19, 0), 2)
                .andExpect(status().isCreated()).andReturn();
        long id = read(created).get("id").asLong();

        mvc.perform(post("/api/bookings/" + id + "/cancel").header("Authorization", bearer(registerCustomer())))
                .andExpect(status().isNotFound());
        // an admin may
        mvc.perform(post("/api/bookings/" + id + "/cancel").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void myBookingsListsOnlyMyOwn() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        addTable(admin, restaurant, "T1", 4);
        addTable(admin, restaurant, "T2", 4);
        String mine = registerCustomer();
        book(mine, restaurant, tomorrowAt(19, 0), 2).andExpect(status().isCreated());
        book(registerCustomer(), restaurant, tomorrowAt(19, 0), 2).andExpect(status().isCreated());

        MvcResult r = mvc.perform(get("/api/bookings/my").header("Authorization", bearer(mine)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, read(r).size());
    }

    private boolean slotAvailable(long restaurantId, LocalDateTime slot) throws Exception {
        MvcResult r = mvc.perform(get("/api/restaurants/" + restaurantId + "/availability")
                        .param("date", slot.toLocalDate().toString()).param("partySize", "2"))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode s : read(r).get("slots")) {
            if (LocalDateTime.parse(s.get("startsAt").asText()).equals(slot)) {
                return s.get("available").asBoolean();
            }
        }
        throw new AssertionError("slot not offered: " + slot);
    }
}
