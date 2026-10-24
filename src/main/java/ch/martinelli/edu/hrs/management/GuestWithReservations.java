package ch.martinelli.edu.hrs.management;

import java.util.List;

/**
 * The guest with all their reservations (UC-024 step 3).
 */
public record GuestWithReservations(ErasableGuest guest, List<GuestReservation> reservations) {

    public List<GuestReservation> openReservations() {
        return reservations.stream().filter(GuestReservation::open).toList();
    }
}
