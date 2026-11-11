package com.bookit.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available = true;

    protected MenuItem() {}

    public MenuItem(Restaurant restaurant, String name, String description, String category, BigDecimal price) {
        this.restaurant = restaurant;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
    }

    public void update(String name, String description, String category, BigDecimal price, boolean available) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.available = available;
    }

    public Long getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
