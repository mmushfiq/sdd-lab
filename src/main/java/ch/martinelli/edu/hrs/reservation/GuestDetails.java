package ch.martinelli.edu.hrs.reservation;

/**
 * The personal data a guest enters for an online reservation.
 */
// UC-001 BR-008: Only first name, last name, email address and phone number are collected.
public record GuestDetails(String firstName, String lastName, String email, String phone) {
}
