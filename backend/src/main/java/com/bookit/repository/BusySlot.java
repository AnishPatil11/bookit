package com.bookit.repository;

import java.time.LocalDateTime;

/** A confirmed reservation window on one table (projection used for availability maths). */
public record BusySlot(Long tableId, LocalDateTime startsAt, LocalDateTime endsAt) {

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startsAt.isBefore(end) && endsAt.isAfter(start);
    }
}
