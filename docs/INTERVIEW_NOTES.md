# BookIt: interview notes

Read this before an interview. Everything here is something you can explain because it is in the code.

## 30-second pitch

"BookIt is a restaurant booking and pre-ordering platform. Spring Boot REST API on PostgreSQL, React and TypeScript frontend. The interesting part is the booking engine: it uses a row lock and an overlap check inside a transaction, plus a PostgreSQL exclusion constraint as a last line of defence, so two people can never hold the same table. I wrote a concurrency test that sends eight simultaneous requests and asserts only one succeeds."

## Questions you will be asked, and the honest answers

**How do you prevent double booking?**
Three layers. (1) `SELECT ... FOR UPDATE` on the restaurant row serialises booking creation per restaurant. (2) Inside the lock I query confirmed bookings overlapping the window and choose a free table. (3) On PostgreSQL, an exclusion constraint on `(dining_table_id, tsrange(starts_at, ends_at))` where status is CONFIRMED makes the database reject overlaps even if someone bypasses the service. `BookingConcurrencyTest` proves layers 1 and 2 on H2.

**Why pessimistic locking and not optimistic?**
Bookings are a contention hotspot where a conflict means a failed customer experience, and the critical section is short. Optimistic locking suits rarely-conflicting updates; I use `@Version` for those (changing booking or order status).

**Why lock the restaurant row instead of the table row?**
Choosing a table means reading all of a restaurant's tables and bookings, so I need to serialise the whole decision. Locking per restaurant is simple and correct; the trade-off is throughput on one very busy restaurant. A finer design could lock candidate table rows in a fixed order.

**What is the overlap condition?**
Two windows overlap when `a.start < b.end AND a.end > b.start`. Back-to-back bookings (one ends exactly when the next starts) do not overlap. `BusySlotTest` covers this.

**Why Flyway and `ddl-auto=validate`?**
Schema changes must be versioned, reviewable and repeatable across environments. Hibernate only checks that entities match the schema, so a mismatch fails at startup, not in production at 2 a.m.

**Why DTO records instead of returning entities?**
Entities have lazy relations and internal fields (password hash). DTOs fix the API contract, avoid lazy-loading surprises with `open-in-view=false`, and let the API evolve independently of the schema.

**How did you avoid N+1 queries?**
List queries use `join fetch` for the associations the response needs, and `default_batch_fetch_size` batches the order-items collection loads.

**How does authentication work?**
Register and login return a signed JWT (HS256, 2-hour expiry). A filter validates it on each request, then loads the user from the database, so role changes and deleted accounts apply immediately. Passwords use BCrypt. Admin routes require `ROLE_ADMIN` in `SecurityConfig`.

**Where is authorisation enforced?**
On the server, twice: URL rules for `/api/admin/**`, and ownership checks in services. A customer asking for someone else's booking gets 404 so ids cannot be probed. The React route guards are only for user experience.

**How do you handle errors?**
One `@RestControllerAdvice` converts exceptions into RFC 7807 problem responses. Validation errors include a per-field map the frontend shows.

**Why snapshot name and price on order items?**
So editing or deleting a menu item never changes past orders. Totals are computed on the server; the client price is never trusted.

**How is it tested?**
Unit tests (state machine, interval maths, JWT), integration tests with MockMvc against the real Spring context and Flyway migration on H2, and the concurrency test. CI runs them on every push.

**What are the weaknesses?**
H2 tests do not exercise the PostgreSQL exclusion constraint (next step: Testcontainers). JWT lives in localStorage (next step: httpOnly cookie plus refresh token). One admin role manages all restaurants. No rate limiting on login.

## Be ready to demo

1. Open two browser windows, book the same slot in both, show the second gets a clean error.
2. Show Swagger UI and call an admin endpoint with and without the token.
3. Show `V2__prevent_overlapping_bookings.sql` and explain it in one sentence.
4. Move an order through the kitchen states as admin and show an illegal jump rejected.

## Resume entry (use after you have run it and deployed it)

**BookIt - Restaurant Booking and Ordering Platform** | Java, Spring Boot, PostgreSQL, React, TypeScript, Redux Toolkit, Docker
- Built a REST API with JWT authentication and role-based access (customer, admin) covering restaurants, tables, menus, bookings and orders.
- Prevented double bookings with transactional row locking, overlap validation and a PostgreSQL exclusion constraint; verified with a concurrent-request integration test.
- Modelled the schema with Flyway migrations, used DTOs, pagination and RFC 7807 error responses; documented the API with OpenAPI.
- Wrote JUnit and MockMvc tests, containerised with Docker Compose and set up GitHub Actions CI; deployed on Render, Neon and Vercel.

Only keep a line if it is true for your final version, and be ready to explain every word.
