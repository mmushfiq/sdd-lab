package ch.martinelli.edu.hrs.management;

import java.math.BigDecimal;

/**
 * A room type in the manager's list (UC-016 step 2), with the number of its active rooms.
 */
public record RoomTypeOverview(Long id, String name, String description, int maxGuests, BigDecimal nightlyRate,
                               boolean active, int activeRooms) {
}
