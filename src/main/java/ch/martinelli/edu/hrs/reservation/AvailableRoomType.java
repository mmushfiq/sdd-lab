package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * A room type that is available for every night of a searched date range, with the total price for that range.
 */
public record AvailableRoomType(Long roomTypeId, String name, String description, int maxGuests,
                                BigDecimal nightlyRate, long nights, BigDecimal totalPrice) {
}
