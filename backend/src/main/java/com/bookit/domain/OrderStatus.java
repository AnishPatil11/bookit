package com.bookit.domain;

/** Kitchen workflow. A status can only move forward along the allowed edges below. */
public enum OrderStatus {
    PLACED, PREPARING, READY, SERVED, CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == READY || next == CANCELLED;
            case READY -> next == SERVED;
            case SERVED, CANCELLED -> false;
        };
    }

    public boolean isOpen() {
        return this == PLACED || this == PREPARING || this == READY;
    }
}
