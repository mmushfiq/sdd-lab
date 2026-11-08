package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;

import java.util.List;

/**
 * A reservation found by reception, with its room number and saved night rates for display (FR-013, 003 R6).
 */
public record FoundReservation(ReservationRecord reservation, String roomNumber, List<NightRate> nights) {
}
