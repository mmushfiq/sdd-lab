package ch.martinelli.edu.hrs.management;

import java.util.List;

/**
 * The guest still has Confirmed or Checked In reservations (UC-024 A2).
 */
public class GuestHasOpenReservationsException extends RuntimeException {

    private final transient List<GuestReservation> openReservations;

    public GuestHasOpenReservationsException(List<GuestReservation> openReservations) {
        super("The guest has open reservations");
        this.openReservations = List.copyOf(openReservations);
    }

    public List<GuestReservation> openReservations() {
        return openReservations;
    }
}
