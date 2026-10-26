package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.RetryingEmailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sends the confirmation email of a reservation and retries it while the Email Service does not accept it
 * (UC-001 step 10 and A8, UC-004 step 11).
 */
@Component
public class ConfirmationEmailSender {

    private final RetryingEmailSender emailSender;

    public ConfirmationEmailSender(RetryingEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Sends the confirmation email right away.
     *
     * @return true when the Email Service accepted it; false when it did not and the email will be retried
     */
    // UC-001 BR-011: The confirmation email is sent right after the reservation is recorded.
    // UC-001 BR-012: Retry for at most 24 hours after the reservation was recorded; never change the reservation.
    public boolean send(String guestEmail, String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                        String roomTypeName, int numberOfGuests, BigDecimal totalPrice,
                        LocalDateTime onlineCancellationDeadline, LocalDateTime reservationRecordedAt) {
        return emailSender.send(confirmationEmail(guestEmail, reservationCode, arrivalDate, departureDate,
                roomTypeName, numberOfGuests, totalPrice, onlineCancellationDeadline, reservationRecordedAt),
                reservationRecordedAt);
    }

    // UC-001 BR-011: The email contains reservation code, arrival date, departure date, room type, number of
    // guests, total price and the online cancellation deadline.
    // UC-003 BR-006: The deadline as date and time in local time, or that only reception can still cancel.
    static Email confirmationEmail(String guestEmail, String reservationCode, LocalDate arrivalDate,
                                   LocalDate departureDate, String roomTypeName, int numberOfGuests,
                                   BigDecimal totalPrice, LocalDateTime onlineCancellationDeadline,
                                   LocalDateTime reservationRecordedAt) {
        String body = """
                Thank you for your reservation.

                Reservation code: %s
                Arrival date: %s
                Departure date: %s
                Room type: %s
                Number of guests: %d
                Total price: CHF %s
                %s

                You pay at the hotel. Keep your reservation code to view or cancel your reservation.
                """.formatted(reservationCode, arrivalDate, departureDate, roomTypeName, numberOfGuests,
                totalPrice.setScale(2).toPlainString(),
                OnlineCancellationDeadline.describe(onlineCancellationDeadline, reservationRecordedAt));
        return new Email(guestEmail, "Your reservation " + reservationCode, body);
    }
}
