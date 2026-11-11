package com.bookit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bookit.domain.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void happyPathMovesForwardOneStepAtATime() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY));
        assertTrue(OrderStatus.READY.canTransitionTo(OrderStatus.SERVED));
    }

    @Test
    void cannotSkipOrGoBackwards() {
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.READY));
        assertFalse(OrderStatus.PLACED.canTransitionTo(OrderStatus.SERVED));
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.PREPARING));
    }

    @Test
    void cancellationOnlyBeforeReady() {
        assertTrue(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void finishedOrdersAreFrozen() {
        for (OrderStatus next : OrderStatus.values()) {
            assertFalse(OrderStatus.SERVED.canTransitionTo(next));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(next));
        }
        assertEquals(3, java.util.Arrays.stream(OrderStatus.values()).filter(OrderStatus::isOpen).count());
    }
}
