package ch.martinelli.edu.hrs.guestbooking;

import java.math.BigDecimal;

/**
 * A bookable room type for a stay, without room numbers.
 */
public record RoomTypeAvailability(String roomTypeName, int capacity, BigDecimal nightlyRate, BigDecimal totalPrice,
                                   int freeRooms) {
}
