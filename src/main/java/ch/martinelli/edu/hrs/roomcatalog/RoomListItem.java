package ch.martinelli.edu.hrs.roomcatalog;

import java.math.BigDecimal;

public record RoomListItem(long id, String number, long roomTypeId, String roomTypeName, int capacity,
                           BigDecimal nightlyRate, boolean outOfService) {
}
