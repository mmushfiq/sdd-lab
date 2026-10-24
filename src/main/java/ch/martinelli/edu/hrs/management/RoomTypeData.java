package ch.martinelli.edu.hrs.management;

import java.math.BigDecimal;

/**
 * The values a manager enters for a room type (UC-016 step 4).
 */
public record RoomTypeData(String name, String description, Integer maxGuests, BigDecimal nightlyRate,
                           boolean active) {
}
