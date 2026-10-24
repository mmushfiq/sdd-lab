package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.RetryingEmailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sends the cancellation email of a reservation and retries it while the Email Service does not accept it
 * (UC-003 step 10 and A8).
 */
@Component
public class CancellationEmailSender {

    private final RetryingEmailSender emailSender;

    public CancellationEmailSender(RetryingEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * @return true when the Email Service accepted it; false when it did not and the email will be retried
     */
    // UC-003 BR-004: Sent right after the cancellation is recorded, retried for at most 24 hours.
    public boolean send(String guestEmail, String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                        String roomTypeName, LocalDateTime cancelledAt) {
        return emailSender.send(cancellationEmail(guestEmail, reservationCode, arrivalDate, departureDate,
                roomTypeName), cancelledAt);
    }

    // UC-003 step 10: The email contains reservation code, dates and room type.
    static Email cancellationEmail(String guestEmail, String reservationCode, LocalDate arrivalDate,
                                   LocalDate departureDate, String roomTypeName) {
        String body = """
                Your reservation has been cancelled free of charge.

                Reservation code: %s
                Arrival date: %s
                Departure date: %s
                Room type: %s
                """.formatted(reservationCode, arrivalDate, departureDate, roomTypeName);
        return new Email(guestEmail, "Your reservation " + reservationCode + " is cancelled", body);
    }
}
