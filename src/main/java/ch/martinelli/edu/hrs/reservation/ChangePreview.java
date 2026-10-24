package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

/**
 * The result of checking the new values (UC-005 step 5): the old and the new total price, and whether any value
 * differs from the reservation.
 */
public record ChangePreview(BigDecimal oldTotalPrice, BigDecimal newTotalPrice, String newRoomTypeName,
                            boolean changed) {
}
