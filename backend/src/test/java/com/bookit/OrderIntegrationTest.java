package com.bookit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class OrderIntegrationTest extends IntegrationTestBase {

    private ResultActions order(String token, long restaurantId, List<Map<String, Object>> lines) throws Exception {
        Map<String, Object> body = Map.of("restaurantId", restaurantId, "items", lines);
        return mvc.perform(post("/api/orders").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static Map<String, Object> line(long menuItemId, int quantity) {
        return Map.of("menuItemId", menuItemId, "quantity", quantity);
    }

    @Test
    void totalIsComputedOnTheServerAndDuplicateLinesAreMerged() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        long dosa = addMenuItem(admin, restaurant, "Dosa", "100.00", true);
        long coffee = addMenuItem(admin, restaurant, "Coffee", "50.50", true);

        order(registerCustomer(), restaurant, List.of(line(dosa, 1), line(dosa, 1), line(coffee, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(250.50))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void unavailableOrForeignMenuItemsAreRejected() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        long other = createRestaurant(admin);
        long sold = addMenuItem(admin, restaurant, "Sold out", "10.00", false);
        long foreign = addMenuItem(admin, other, "Elsewhere", "10.00", true);
        String customer = registerCustomer();

        order(customer, restaurant, List.of(line(sold, 1))).andExpect(status().isBadRequest());
        order(customer, restaurant, List.of(line(foreign, 1))).andExpect(status().isBadRequest());
        order(customer, restaurant, List.of(line(987_654L, 1))).andExpect(status().isBadRequest());
        order(customer, restaurant, List.of()).andExpect(status().isBadRequest());
    }

    @Test
    void kitchenWorkflowAndCancellationRules() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        long item = addMenuItem(admin, restaurant, "Thali", "200.00", true);
        String customer = registerCustomer();

        MvcResult placed = order(customer, restaurant, List.of(line(item, 1))).andExpect(status().isCreated()).andReturn();
        long id = read(placed).get("id").asLong();
        String statusUrl = "/api/admin/orders/" + id + "/status";

        // cannot jump PLACED -> READY
        mvc.perform(post(statusUrl).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isBadRequest());
        // PLACED -> PREPARING works, and from then on the customer can no longer cancel
        mvc.perform(post(statusUrl).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PREPARING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));
        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer(customer)))
                .andExpect(status().isBadRequest());
        // customers cannot drive the kitchen
        mvc.perform(post(statusUrl).header("Authorization", bearer(customer)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanCancelAnUntouchedOrderAndSeesOnlyTheirOwn() throws Exception {
        String admin = adminToken();
        long restaurant = createRestaurant(admin);
        long item = addMenuItem(admin, restaurant, "Soup", "90.00", true);
        String customer = registerCustomer();

        MvcResult placed = order(customer, restaurant, List.of(line(item, 2))).andExpect(status().isCreated()).andReturn();
        order(registerCustomer(), restaurant, List.of(line(item, 1))).andExpect(status().isCreated());

        long id = read(placed).get("id").asLong();
        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/orders/my").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        // somebody else's order id looks like it does not exist
        mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", bearer(registerCustomer())))
                .andExpect(status().isNotFound());
    }
}
