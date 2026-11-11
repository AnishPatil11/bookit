package com.bookit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Shared plumbing: full Spring context on in-memory H2 with the real Flyway migration. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"h2", "test"})
public abstract class IntegrationTestBase {

    protected static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    /** Registers a fresh customer and returns their JWT. */
    protected String registerCustomer() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.dev";
        String body = json.writeValueAsString(Map.of("name", "Test User", "email", email, "password", "Password@123"));
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return read(r).get("token").asText();
    }

    protected String adminToken() throws Exception {
        String body = json.writeValueAsString(Map.of("email", "admin@bookit.dev", "password", "Admin@12345"));
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return read(r).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Creates an active restaurant (open 10:00-23:00) through the admin API and returns its id. */
    protected long createRestaurant(String adminToken) throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Test Bistro " + UUID.randomUUID(), "cuisine", "Fusion", "city", "Hubballi",
                "openTime", "10:00", "closeTime", "23:00", "description", "test", "address", "1 Test Road");
        MvcResult r = mvc.perform(post("/api/admin/restaurants").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return read(r).get("id").asLong();
    }

    protected void addTable(String adminToken, long restaurantId, String label, int capacity) throws Exception {
        mvc.perform(post("/api/admin/restaurants/" + restaurantId + "/tables")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("label", label, "capacity", capacity))))
                .andExpect(status().isCreated());
    }

    protected long addMenuItem(String adminToken, long restaurantId, String name, String price, boolean available)
            throws Exception {
        MvcResult r = mvc.perform(post("/api/admin/restaurants/" + restaurantId + "/menu-items")
                        .header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "category", "Mains",
                                "price", price, "available", available))))
                .andExpect(status().isCreated()).andReturn();
        return read(r).get("id").asLong();
    }

    /** A valid booking start: tomorrow at the given hour (on the 30-minute slot grid). */
    protected static LocalDateTime tomorrowAt(int hour, int minute) {
        return LocalDate.now(ZONE).plusDays(1).atTime(hour, minute);
    }

    protected Map<String, Object> bookingBody(long restaurantId, LocalDateTime startsAt, int partySize) {
        return Map.of("restaurantId", restaurantId, "startsAt", startsAt.toString(), "partySize", partySize);
    }
}
