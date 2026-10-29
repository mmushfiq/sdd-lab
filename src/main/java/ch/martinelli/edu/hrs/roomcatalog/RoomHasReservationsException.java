package ch.martinelli.edu.hrs.roomcatalog;

public class RoomHasReservationsException extends RuntimeException {

    public RoomHasReservationsException() {
        super("Room has reservations and cannot be deleted; set it out of service instead");
    }
}
