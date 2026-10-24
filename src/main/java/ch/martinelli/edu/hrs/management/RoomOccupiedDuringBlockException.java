package ch.martinelli.edu.hrs.management;

/**
 * The room is assigned to a Checked In reservation whose stay includes a blocked night (UC-018 A3, BR-002).
 */
public class RoomOccupiedDuringBlockException extends RuntimeException {

    private final String reservationCode;

    public RoomOccupiedDuringBlockException(String reservationCode) {
        super("The room is occupied by reservation " + reservationCode);
        this.reservationCode = reservationCode;
    }

    public String reservationCode() {
        return reservationCode;
    }
}
