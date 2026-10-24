package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.EmailService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends the confirmation email of an online reservation and retries it while the Email Service does not accept it
 * (UC-001 step 10 and A8). Retries are kept in memory and are lost when the application restarts.
 */
@Component
public class ConfirmationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationEmailSender.class);

    static final Duration RETRY_WINDOW = Duration.ofHours(24);
    static final Duration RETRY_INTERVAL = Duration.ofMinutes(1);

    private final EmailService emailService;
    private final Clock clock;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("confirmation-email-retry").daemon().factory());

    public ConfirmationEmailSender(EmailService emailService, Clock clock) {
        this.emailService = emailService;
        this.clock = clock;
    }

    /**
     * Sends the confirmation email right away.
     *
     * @return true when the Email Service accepted it; false when it did not and the email will be retried
     */
    // UC-001 BR-011: The confirmation email is sent right after the reservation is recorded.
    public boolean send(String guestEmail, String reservationCode, LocalDate arrivalDate, LocalDate departureDate,
                        String roomTypeName, int numberOfGuests, BigDecimal totalPrice,
                        LocalDateTime reservationRecordedAt) {
        Email email = confirmationEmail(guestEmail, reservationCode, arrivalDate, departureDate, roomTypeName,
                numberOfGuests, totalPrice);
        if (trySend(email)) {
            return true;
        }
        scheduleRetry(email, reservationRecordedAt);
        return false;
    }

    // UC-001 BR-011: The email contains reservation code, arrival date, departure date, room type, number of guests and total price.
    static Email confirmationEmail(String guestEmail, String reservationCode, LocalDate arrivalDate,
                                   LocalDate departureDate, String roomTypeName, int numberOfGuests,
                                   BigDecimal totalPrice) {
        String body = """
                Thank you for your reservation.

                Reservation code: %s
                Arrival date: %s
                Departure date: %s
                Room type: %s
                Number of guests: %d
                Total price: CHF %s

                You pay at the hotel. Keep your reservation code to view or cancel your reservation.
                """.formatted(reservationCode, arrivalDate, departureDate, roomTypeName, numberOfGuests,
                totalPrice.setScale(2).toPlainString());
        return new Email(guestEmail, "Your reservation " + reservationCode, body);
    }

    // UC-001 BR-012: Retry for at most 24 hours after the reservation was recorded; never change the reservation.
    private void scheduleRetry(Email email, LocalDateTime reservationRecordedAt) {
        LocalDateTime retryDeadline = reservationRecordedAt.plus(RETRY_WINDOW);
        scheduler.schedule(() -> {
            if (!LocalDateTime.now(clock).isBefore(retryDeadline)) {
                log.warn("Gave up sending the confirmation email '{}' after {}", email.subject(), RETRY_WINDOW);
                return;
            }
            if (!trySend(email)) {
                scheduleRetry(email, reservationRecordedAt);
            }
        }, RETRY_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private boolean trySend(Email email) {
        try {
            emailService.send(email);
            return true;
        } catch (RuntimeException e) {
            log.warn("The Email Service did not accept the confirmation email '{}': {}", email.subject(),
                    e.getMessage());
            return false;
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
