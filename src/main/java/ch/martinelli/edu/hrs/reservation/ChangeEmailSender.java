package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.RetryingEmailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sends the change confirmation email of a changed reservation and retries it while the Email Service does not
 * accept it (UC-005 step 8 and A8).
 */
@Component
public class ChangeEmailSender {

    private final RetryingEmailSender emailSender;

    public ChangeEmailSender(RetryingEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * @return true when the Email Service accepted it; false when it did not and the email will be retried
     */
    // UC-005 BR-006: Sent right after the change is recorded, retried for at most 24 hours; never undoes the change.
    public boolean send(String guestEmail, String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                        String roomTypeName, int numberOfGuests, BigDecimal totalPrice, LocalDateTime changedAt) {
        return emailSender.send(changeEmail(guestEmail, reservationCode, arrivalDate, departureDate, roomTypeName,
                numberOfGuests, totalPrice), changedAt);
    }

    // UC-005 step 8: The email contains the reservation code and the new dates, room type, number of guests and total price.
    static Email changeEmail(String guestEmail, String reservationCode, LocalDate arrivalDate,
                             LocalDate departureDate, String roomTypeName, int numberOfGuests,
                             BigDecimal totalPrice) {
        String body = """
                Your reservation has been changed.

                Reservation code: %s
                Arrival date: %s
                Departure date: %s
                Room type: %s
                Number of guests: %d
                Total price: CHF %s

                You pay at the hotel.
                """.formatted(reservationCode, arrivalDate, departureDate, roomTypeName, numberOfGuests,
                totalPrice.setScale(2).toPlainString());
        return new Email(guestEmail, "Your reservation " + reservationCode + " has been changed", body);
    }
}
