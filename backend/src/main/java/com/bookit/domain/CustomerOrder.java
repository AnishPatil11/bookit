package com.bookit.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Named CustomerOrder because ORDER is a reserved word in SQL and JPQL. */
@Entity(name = "CustomerOrder")
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(length = 300)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {}

    public CustomerOrder(User user, Restaurant restaurant, Booking booking, String notes, LocalDateTime createdAt) {
        this.user = user;
        this.restaurant = restaurant;
        this.booking = booking;
        this.notes = notes;
        this.createdAt = createdAt;
        this.status = OrderStatus.PLACED;
        this.total = BigDecimal.ZERO;
    }

    /** Adds a line and keeps the order total in step. */
    public void addItem(OrderItem item) {
        items.add(item);
        total = total.add(item.lineTotal());
    }

    public void setStatus(OrderStatus status) { this.status = status; }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Restaurant getRestaurant() { return restaurant; }
    public Booking getBooking() { return booking; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
