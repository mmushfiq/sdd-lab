package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;

/**
 * A reservation found by reception, with its room number for display (FR-013).
 */
public record FoundReservation(ReservationRecord reservation, String roomNumber) {
}
