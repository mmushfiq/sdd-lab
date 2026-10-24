package ch.martinelli.edu.hrs.reservation;

/**
 * Thrown when the selected room type was taken for at least one night of the date range since the search (UC-001 A5).
 */
public class RoomTypeNoLongerAvailableException extends RuntimeException {

    public RoomTypeNoLongerAvailableException() {
        super("The selected room type is no longer available for these dates");
    }
}
