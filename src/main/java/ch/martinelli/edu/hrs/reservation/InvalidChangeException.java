package ch.martinelli.edu.hrs.reservation;

/**
 * A new value violates a rule of UC-005 (A3); {@code field} names the value that is invalid.
 */
public class InvalidChangeException extends IllegalArgumentException {

    public enum Field { ARRIVAL_DATE, DEPARTURE_DATE, ROOM_TYPE, NUMBER_OF_GUESTS }

    private final Field field;

    public InvalidChangeException(Field field, String message) {
        super(message);
        this.field = field;
    }

    public Field field() {
        return field;
    }
}
