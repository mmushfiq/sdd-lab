package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * A free room of a search; the total price is the sum of the stay's night rates (003 FR-005).
 */
public record AvailableRoom(long roomId, String roomNumber, String roomTypeName, int capacity,
                            BigDecimal totalPrice) {
}
