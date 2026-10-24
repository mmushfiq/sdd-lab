package ch.martinelli.edu.hrs.reservation;

/**
 * The result of an online cancellation. {@code cancellationEmailAccepted} is false when the Email Service did not
 * accept the cancellation email on the first attempt and it is being retried (UC-003 A8).
 */
public record OnlineCancellation(OwnReservation reservation, boolean cancellationEmailAccepted) {
}
