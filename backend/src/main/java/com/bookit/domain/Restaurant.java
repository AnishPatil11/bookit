package com.bookit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 60)
    private String cuisine;

    @Column(length = 200)
    private String address;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Restaurant() {}

    public Restaurant(String name, String description, String cuisine, String address, String city,
                      LocalTime openTime, LocalTime closeTime, String imageUrl, LocalDateTime createdAt) {
        this.name = name;
        this.description = description;
        this.cuisine = cuisine;
        this.address = address;
        this.city = city;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public void update(String name, String description, String cuisine, String address, String city,
                       LocalTime openTime, LocalTime closeTime, String imageUrl, boolean active) {
        this.name = name;
        this.description = description;
        this.cuisine = cuisine;
        this.address = address;
        this.city = city;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCuisine() { return cuisine; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public String getImageUrl() { return imageUrl; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
