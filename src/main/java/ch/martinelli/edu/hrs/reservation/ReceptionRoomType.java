package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * A room type the receptionist can reserve for a date range, with the lowest and the highest nightly rate of the
 * nights, the total price for the range and the number of rooms still available on every night (UC-004 step 3).
 */
public record ReceptionRoomType(Long roomTypeId, String name, int maxGuests, BigDecimal lowestNightlyRate,
                                BigDecimal highestNightlyRate, long nights, BigDecimal totalPrice,
                                long availableRooms) {
}
