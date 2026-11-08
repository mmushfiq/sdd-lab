package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 002 FR-006: plain-text confirmation email after an online booking (contracts/confirmation-email.md). Called
 * after the booking is committed; a mail failure is logged and never undoes the booking (research R4).
 */
@Service
public class BookingConfirmationEmail {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationEmail.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String from;

    public BookingConfirmationEmail(JavaMailSender mailSender, Clock clock, @Value("${hrs.mail.from}") String from) {
        this.mailSender = mailSender;
        this.clock = clock;
        this.from = from;
    }

    public void send(ReservationRecord reservation, String roomTypeName) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(reservation.getGuestEmail());
        message.setSubject("Your reservation " + reservation.getReservationNumber());
        message.setText(body(reservation, roomTypeName));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Booking confirmation email for reservation {} could not be sent: {}",
                    reservation.getReservationNumber(), e.getMessage());
        }
    }

    private String body(ReservationRecord reservation, String roomTypeName) {
        var deadline = CancellationDeadline.of(reservation.getArrivalDate(), reservation.getCancellationHoursBefore());
        // A booking made within 48 hours of check-in is already past its deadline
        var deadlineText = deadline.allowsOnlineCancellationAt(LocalDateTime.now(clock.withZone(CancellationDeadline.ZONE)))
                ? deadline.text() : deadline.passedText();
        return """
                Thank you for your booking.

                Reservation number: %s
                Room type: %s
                Arrival: %s
                Departure: %s
                Guests: %d
                Total price: %s

                %s

                View or cancel your reservation online with your reservation number and this email.
                """.formatted(reservation.getReservationNumber(), roomTypeName, date(reservation.getArrivalDate()),
                date(reservation.getDepartureDate()), reservation.getNumberOfGuests(),
                reservation.getTotalPrice().toPlainString(), deadlineText);
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }
}
