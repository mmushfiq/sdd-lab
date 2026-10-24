package ch.martinelli.edu.hrs.reservation;

import java.util.List;

/**
 * The matches of a reservation search; {@code moreMatches} is true when more reservations match than are listed
 * (UC-007 A3).
 */
public record ReservationSearchResults(List<ReservationSearchResult> reservations, boolean moreMatches) {
}
