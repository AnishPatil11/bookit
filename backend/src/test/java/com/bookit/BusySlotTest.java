package com.bookit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bookit.repository.BusySlot;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BusySlotTest {

    private static final LocalDateTime SEVEN = LocalDateTime.of(2030, 1, 1, 19, 0);
    private final BusySlot busy = new BusySlot(1L, SEVEN, SEVEN.plusMinutes(90));

    @Test
    void overlappingWindowsCollide() {
        assertTrue(busy.overlaps(SEVEN.plusMinutes(30), SEVEN.plusMinutes(120)));
        assertTrue(busy.overlaps(SEVEN.minusMinutes(30), SEVEN.plusMinutes(30)));
    }

    @Test
    void backToBackWindowsDoNotCollide() {
        assertFalse(busy.overlaps(SEVEN.plusMinutes(90), SEVEN.plusMinutes(180)));
        assertFalse(busy.overlaps(SEVEN.minusMinutes(90), SEVEN));
    }
}
