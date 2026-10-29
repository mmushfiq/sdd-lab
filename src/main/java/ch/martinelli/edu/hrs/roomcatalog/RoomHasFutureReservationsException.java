package ch.martinelli.edu.hrs.roomcatalog;

import java.util.List;

public class RoomHasFutureReservationsException extends RuntimeException {

    public RoomHasFutureReservationsException(List<String> bookingNumbers) {
        super("Room has future reservations and cannot be set out of service: " + String.join(", ", bookingNumbers));
    }
}
