package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * A room type a reservation can be changed to, with its current nightly rate in CHF (UC-005 step 3).
 */
public record RoomTypeChoice(Long roomTypeId, String name, Integer maxGuests, BigDecimal nightlyRate,
                             Boolean active) {
}
