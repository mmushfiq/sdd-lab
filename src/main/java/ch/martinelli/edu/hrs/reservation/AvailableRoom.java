package ch.martinelli.edu.hrs.reservation;

import java.math.BigDecimal;

public record AvailableRoom(long roomId, String number, String roomTypeName, int capacity, BigDecimal nightlyRate,
                            BigDecimal totalPrice) {
}
