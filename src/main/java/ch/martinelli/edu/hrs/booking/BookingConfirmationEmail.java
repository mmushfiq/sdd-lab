package ch.martinelli.edu.hrs.booking;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static ch.martinelli.edu.hrs.room.ui.RoomTypeView.formatRate;

/**
 * Sends the guest a plain-text confirmation of an online booking. Called after the booking is saved, so a mail
 * failure is only logged and never undoes the booking.
 */
@Service
public class BookingConfirmationEmail {

    private static final Logger LOG = LoggerFactory.getLogger(BookingConfirmationEmail.class);

    private final JavaMailSender mailSender;
    private final String from;

    public BookingConfirmationEmail(JavaMailSender mailSender, @Value("${hrs.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(ReservationRecord reservation, String roomTypeName) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(reservation.getGuestEmail());
        mail.setSubject("Your reservation " + reservation.getReservationNumber());
        mail.setText("""
                Dear %s

                Thank you for your booking.

                Reservation number: %s
                Room type: %s
                Arrival: %s
                Departure: %s
                Guests: %d
                Total price: %s

                %s

                You need your reservation number and this email address to view or cancel your reservation.
                """.formatted(reservation.getGuestName(), reservation.getReservationNumber(), roomTypeName,
                reservation.getArrivalDate(), reservation.getDepartureDate(), reservation.getNumberOfGuests(),
                formatRate(AvailabilityService.totalPrice(reservation.getNightlyRate(),
                        reservation.getArrivalDate(), reservation.getDepartureDate())),
                ReservationService.cancellationText(ReservationService.onlineCancellationDeadline(reservation))));
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            LOG.warn("Booking confirmation for reservation {} not sent", reservation.getReservationNumber(), e);
        }
    }
}
