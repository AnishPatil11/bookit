package com.bookit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registerThenLoginThenMe() throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@test.dev";
        String body = json.writeValueAsString(Map.of("name", "Flow User", "email", email, "password", "Password@123"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.email").value(email));

        String loginBody = json.writeValueAsString(Map.of("email", email, "password", "Password@123"));
        String token = read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk()).andReturn()).get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void duplicateEmailIsRejectedWithConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.dev";
        String body = json.writeValueAsString(Map.of("name", "Dup", "email", email, "password", "Password@123"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        String body = json.writeValueAsString(Map.of("email", "admin@bookit.dev", "password", "wrong-password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        String body = json.writeValueAsString(Map.of("name", "", "email", "not-an-email", "password", "short"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void protectedRoutesNeedAToken() throws Exception {
        mvc.perform(get("/api/bookings/my")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/my").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotUseAdminRoutes() throws Exception {
        String token = registerCustomer();
        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(token))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats").header("Authorization", bearer(adminToken()))).andExpect(status().isOk());
    }

    @Test
    void restaurantBrowsingIsPublic() throws Exception {
        mvc.perform(get("/api/restaurants")).andExpect(status().isOk());
    }
}
