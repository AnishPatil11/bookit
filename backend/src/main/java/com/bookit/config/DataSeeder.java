package com.bookit.config;

import com.bookit.domain.DiningTable;
import com.bookit.domain.MenuItem;
import com.bookit.domain.Restaurant;
import com.bookit.domain.Role;
import com.bookit.domain.User;
import com.bookit.repository.DiningTableRepository;
import com.bookit.repository.MenuItemRepository;
import com.bookit.repository.RestaurantRepository;
import com.bookit.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the admin account on first start and, optionally, a demo customer plus three restaurants. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final DiningTableRepository tables;
    private final MenuItemRepository menu;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public DataSeeder(AppProperties props, UserRepository users, RestaurantRepository restaurants,
                      DiningTableRepository tables, MenuItemRepository menu, PasswordEncoder encoder, Clock clock) {
        this.props = props;
        this.users = users;
        this.restaurants = restaurants;
        this.tables = tables;
        this.menu = menu;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now(clock);
        String adminEmail = props.seed().adminEmail().toLowerCase();
        if (!users.existsByEmailIgnoreCase(adminEmail)) {
            users.save(new User("Admin", adminEmail, encoder.encode(props.seed().adminPassword()), Role.ADMIN, now));
            log.info("Created admin account {}", adminEmail);
        }
        if (!props.seed().demoData() || restaurants.count() > 0) {
            return;
        }
        if (!users.existsByEmailIgnoreCase("demo@bookit.dev")) {
            users.save(new User("Demo Customer", "demo@bookit.dev", encoder.encode("Demo@12345"), Role.CUSTOMER, now));
        }

        Restaurant malnad = restaurants.save(new Restaurant("Malnad Kitchen",
                "Coastal and Malnad home cooking: neer dosa, ghee roast and fresh seafood.",
                "South Indian", "12 Club Road, Vidyanagar", "Hubballi",
                LocalTime.of(11, 0), LocalTime.of(22, 30),
                "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=900", now));
        addTables(malnad, new int[]{2, 2, 2, 4, 4, 6});
        addMenu(malnad, new Object[][]{
                {"Neer Dosa with Chicken Ghee Roast", "Lacy rice crepes with a spicy Mangalorean ghee roast", "Mains", "320.00"},
                {"Kori Rotti", "Crisp rice wafers soaked in a coconut chicken curry", "Mains", "360.00"},
                {"Bisi Bele Bath", "Slow-cooked lentil rice with ghee and boondi", "Mains", "210.00"},
                {"Gobi Manchurian", "Crisp cauliflower in a tangy sauce", "Starters", "180.00"},
                {"Mysore Pak", "Gram flour and ghee sweet", "Desserts", "120.00"},
                {"Filter Coffee", "Decoction coffee served in a davara", "Drinks", "60.00"}});

        Restaurant verde = restaurants.save(new Restaurant("Trattoria Verde",
                "Wood-fired pizza and handmade pasta in a garden courtyard.",
                "Italian", "4 Gokul Road", "Hubballi",
                LocalTime.of(12, 0), LocalTime.of(23, 0),
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=900", now));
        addTables(verde, new int[]{2, 2, 4, 4, 4, 8});
        addMenu(verde, new Object[][]{
                {"Margherita Pizza", "San Marzano tomato, fior di latte, basil", "Mains", "420.00"},
                {"Penne Arrabbiata", "Chilli, garlic and tomato", "Mains", "380.00"},
                {"Mushroom Risotto", "Arborio rice with wild mushrooms and parmesan", "Mains", "460.00"},
                {"Bruschetta", "Toasted ciabatta with tomato and basil", "Starters", "240.00"},
                {"Tiramisu", "Mascarpone and espresso", "Desserts", "280.00"},
                {"Sparkling Lemonade", "House-made", "Drinks", "140.00"}});

        Restaurant harbour = restaurants.save(new Restaurant("Harbour Grill",
                "Charcoal grills, tandoor specials and a long beverage list.",
                "Grill & Barbecue", "88 Airport Road", "Dharwad",
                LocalTime.of(18, 0), LocalTime.of(23, 30),
                "https://images.unsplash.com/photo-1544025162-d76694265947?w=900", now));
        addTables(harbour, new int[]{2, 4, 4, 6, 6, 10});
        addMenu(harbour, new Object[][]{
                {"Tandoori Platter", "Chicken tikka, seekh kebab and paneer tikka", "Starters", "540.00"},
                {"Grilled Fish Steak", "Catch of the day with lemon butter", "Mains", "620.00"},
                {"Lamb Chops", "Marinated and charcoal grilled", "Mains", "780.00"},
                {"Butter Naan", "Tandoor baked", "Sides", "70.00"},
                {"Kulfi Falooda", "Pistachio kulfi with rose syrup", "Desserts", "190.00"},
                {"Fresh Lime Soda", "Sweet or salted", "Drinks", "90.00"}});

        log.info("Seeded demo customer and {} restaurants", restaurants.count());
    }

    private void addTables(Restaurant r, int[] capacities) {
        int n = 1;
        for (int capacity : capacities) {
            tables.save(new DiningTable(r, "T" + n++, capacity));
        }
    }

    private void addMenu(Restaurant r, Object[][] rows) {
        List<MenuItem> items = java.util.Arrays.stream(rows)
                .map(row -> new MenuItem(r, (String) row[0], (String) row[1], (String) row[2],
                        new BigDecimal((String) row[3])))
                .toList();
        menu.saveAll(items);
    }
}
