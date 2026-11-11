package com.bookit.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Name and price are copied at order time, so later menu edits never rewrite history. */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {}

    public OrderItem(CustomerOrder order, Long menuItemId, String name, BigDecimal unitPrice, int quantity) {
        this.order = order;
        this.menuItemId = menuItemId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public Long getMenuItemId() { return menuItemId; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
