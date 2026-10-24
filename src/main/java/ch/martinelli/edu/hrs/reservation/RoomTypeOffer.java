package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * A room type as offered to guests, with its current nightly rate in CHF.
 */
public record RoomTypeOffer(Long roomTypeId, String name, String description, Integer maxGuests,
                            BigDecimal nightlyRate) {
}
