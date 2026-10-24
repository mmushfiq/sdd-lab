package ch.martinelli.edu.hrs.management;

import java.time.LocalDate;
import java.util.List;

/**
 * The room type would have more Confirmed and Checked In reservations than available rooms on these nights
 * (UC-017 A3, BR-003).
 */
public class ReservationsWouldLoseRoomException extends RuntimeException {

    private final String roomTypeName;
    private final List<LocalDate> nights;

    public ReservationsWouldLoseRoomException(String roomTypeName, List<LocalDate> nights) {
        super("Reservations of " + roomTypeName + " would lose their room on " + nights);
        this.roomTypeName = roomTypeName;
        this.nights = List.copyOf(nights);
    }

    public String roomTypeName() {
        return roomTypeName;
    }

    public List<LocalDate> nights() {
        return nights;
    }
}
