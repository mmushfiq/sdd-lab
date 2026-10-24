package ch.martinelli.edu.hrs.reservation;

/**
 * The outcome of a confirmed change. {@code changed} is false when nothing was changed (UC-005 A5);
 * {@code emailAccepted} is false when the change confirmation email is being retried (A8).
 */
public record ChangeResult(boolean changed, boolean emailAccepted) {
}
