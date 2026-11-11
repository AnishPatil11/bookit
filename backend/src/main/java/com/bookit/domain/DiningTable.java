package com.bookit.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "dining_tables")
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active = true;

    protected DiningTable() {}

    public DiningTable(Restaurant restaurant, String label, int capacity) {
        this.restaurant = restaurant;
        this.label = label;
        this.capacity = capacity;
    }

    public void update(String label, int capacity, boolean active) {
        this.label = label;
        this.capacity = capacity;
        this.active = active;
    }

    public Long getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public String getLabel() { return label; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }
}
