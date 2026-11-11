package com.bookit.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dining_table_id")
    private DiningTable diningTable;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(length = 300)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Optimistic lock: two concurrent status changes cannot silently overwrite each other. */
    @Version
    private long version;

    protected Booking() {}

    public Booking(User user, Restaurant restaurant, DiningTable diningTable, LocalDateTime startsAt,
                   LocalDateTime endsAt, int partySize, String notes, LocalDateTime createdAt) {
        this.user = user;
        this.restaurant = restaurant;
        this.diningTable = diningTable;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.partySize = partySize;
        this.notes = notes;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Restaurant getRestaurant() { return restaurant; }
    public DiningTable getDiningTable() { return diningTable; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public int getPartySize() { return partySize; }
    public BookingStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
