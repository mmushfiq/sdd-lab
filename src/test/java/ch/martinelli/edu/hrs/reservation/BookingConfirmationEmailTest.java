package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * 002 FR-006, US4 scenarios 1–2, contracts/confirmation-email.md.
 */
@SpringBootTest
class BookingConfirmationEmailTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private BookingConfirmationEmail confirmationEmail;

    @Autowired
    private Clock clock;

    @Test
    void sendsReservationDetailsAndDeadline() {
        var arrival = LocalDate.now(clock).plusDays(30);
        var reservation = reservation(arrival);

        confirmationEmail.send(reservation, "Double");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        var message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("reservations@hotel.example");
        assertThat(message.getTo()).containsExactly("anna@example.com");
        assertThat(message.getSubject()).isEqualTo("Your reservation K7Q2M9XA");
        assertThat(message.getText()).isEqualTo("""
                Thank you for your booking.

                Reservation number: K7Q2M9XA
                Room type: Double
                Arrival: %s
                Departure: %s
                Guests: 2
                Total price: 240.00

                %s

                View or cancel your reservation online with your reservation number and this email.
                """.formatted(BookingConfirmationEmail.date(arrival), BookingConfirmationEmail.date(arrival.plusDays(2)),
                CancellationDeadline.of(arrival, 48).text()));
    }

    @Test
    void bookingWithin48HoursGetsThePassedText() {
        var arrival = LocalDate.now(clock).plusDays(1);

        confirmationEmail.send(reservation(arrival), "Double");

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains(CancellationDeadline.of(arrival, 48).passedText());
    }

    @Test
    void mailFailureIsCaught() {
        doThrow(new MailSendException("mail server down")).when(mailSender).send(any(SimpleMailMessage.class));

        confirmationEmail.send(reservation(LocalDate.now(clock).plusDays(30)), "Double");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private static ReservationRecord reservation(LocalDate arrival) {
        var reservation = new ReservationRecord();
        reservation.setReservationNumber("K7Q2M9XA");
        reservation.setArrivalDate(arrival);
        reservation.setDepartureDate(arrival.plusDays(2));
        reservation.setNumberOfGuests(2);
        reservation.setTotalPrice(new BigDecimal("240.00"));
        reservation.setGuestEmail("anna@example.com");
        reservation.setCancellationHoursBefore(48);
        return reservation;
    }
}
