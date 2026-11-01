package ch.martinelli.edu.hrs.booking.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@Transactional
@WithAnonymousUser
class BookingViewTest extends HrsBrowserlessTest {

    @Autowired
    ReservationService reservationService;

    @Autowired
    DSLContext ctx;

    @MockitoBean
    JavaMailSender mailSender;

    // Scenario "Guest opens the booking page"
    @Test
    void booking_page_opens_without_login() {
        navigate(BookingView.class);

        assertThat($(Button.class).id("search-rooms").isVisible()).isTrue();
    }

    // Scenario "Guest sees free rooms": room type, capacity and total price, no room numbers and no nightly rate
    @Test
    void search_shows_free_rooms_with_total_price_without_room_numbers() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);

        // Rooms 104-108 are Double, 109-110 Family
        assertThat(cards()).hasSize(7);
        assertThat(cardTexts(cards().getFirst()))
                .containsExactly("Up to 2 guests", "Total price: CHF 280.00");
        assertThat(cards().getFirst().getElement().getTextRecursively()).startsWith("Double");
        assertThat(cards().getLast().getElement().getTextRecursively()).startsWith("Family");
        assertThat($(FlexLayout.class).id("room-results").getElement().getTextRecursively())
                .doesNotContainPattern("1[01]\\d");
    }

    // Scenario "Guest sees the seasonal price"
    @Test
    void guest_sees_the_seasonal_total_price() {
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        navigate(BookingView.class);
        search(DECEMBER_18, DECEMBER_21, 2);

        assertThat(cards().getFirst().getElement().getTextRecursively()).startsWith("Double");
        assertThat(cardTexts(cards().getFirst())).contains("Total price: CHF 460.00");
    }

    // Scenario "Guest search with invalid input"
    @Test
    void past_arrival_shows_message_and_does_not_search() {
        navigate(BookingView.class);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        search(yesterday, yesterday.plusDays(2), 2);

        assertThat(message()).isEqualTo("Arrival date cannot be in the past");
        assertThat(cards()).isEmpty();
    }

    // Scenario "No room available for the guest"
    @Test
    void no_room_available_shows_message() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 5);

        assertThat(message()).isEqualTo("No rooms available for these dates");
        assertThat(cards()).isEmpty();
    }

    // Requirement "Privacy notice"
    @Test
    void booking_form_shows_privacy_notice() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();

        assertThat($(Paragraph.class).id("privacy-notice").getText())
                .isEqualTo("We use your name, email and phone number only to manage your reservation.");
    }

    // Scenario "Guest data missing or invalid"
    @Test
    void missing_name_and_email_are_marked() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();
        test($(Button.class).id("book")).click();

        assertThat(field("Name").isInvalid()).isTrue();
        assertThat(field("Name").getErrorMessage()).isEqualTo("Name is required");
        assertThat(field("Email").isInvalid()).isTrue();
        assertThat(field("Email").getErrorMessage()).isEqualTo("Email is required");
        assertThat(ctx.fetchCount(RESERVATION)).isZero();
    }

    // Scenario "Guest data missing or invalid": invalid email
    @Test
    void invalid_email_is_marked() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();
        test(field("Name")).setValue("Anna Meier");
        test(field("Email")).setValue("anna-at-example.com");
        test($(Button.class).id("book")).click();

        assertThat(field("Email").isInvalid()).isTrue();
        assertThat(field("Email").getErrorMessage()).isEqualTo("Enter a valid email address");
        assertThat(ctx.fetchCount(RESERVATION)).isZero();
    }

    // Scenario "Guest books successfully"
    @Test
    void successful_booking_shows_confirmation_with_reservation_number() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();
        test(field("Name")).setValue("Anna Meier");
        test(field("Email")).setValue("anna@example.com");
        test($(Button.class).id("book")).click();

        ReservationRecord saved = ctx.fetchSingle(RESERVATION);
        assertThat(saved.getStatus()).isEqualTo("BOOKED");
        assertThat(saved.getRoomId()).isEqualTo(roomId(ctx, "104"));
        assertThat(saved.getGuestName()).isEqualTo("Anna Meier");
        assertThat($(Span.class).id("reservation-number").getText()).isEqualTo(saved.getReservationNumber());
        assertThat($(Paragraph.class).from($(Div.class).id("booking-confirmation")).all())
                .extracting(Paragraph::getText).contains(
                        "Room type: Double", "Arrival: 2030-03-10", "Departure: 2030-03-12", "Guests: 2",
                        "Total price: CHF 280.00", BookingView.KEEP_NUMBER);
        assertThat($(Button.class).withId("search-rooms").exists()).isFalse();
    }

    // Requirement "Booking confirmation email", scenario "Email sent after booking"
    @Test
    void booking_sends_confirmation_email_with_deadline() {
        book("Anna Meier", "anna@example.com");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        ReservationRecord saved = ctx.fetchSingle(RESERVATION);
        assertThat(sent.getValue().getTo()).containsExactly("anna@example.com");
        assertThat(sent.getValue().getText()).contains(
                "Reservation number: " + saved.getReservationNumber(), "Room type: Double",
                "Arrival: 2030-03-10", "Departure: 2030-03-12", "Guests: 2", "Total price: CHF 280.00",
                "Free online cancellation until 8 March 2030, 14:00 (Zurich time)");
    }

    // Scenario "Email cannot be sent": booking is saved and confirmed anyway
    @Test
    void failed_email_keeps_booking_and_confirmation() {
        doThrow(new MailSendException("mail server down")).when(mailSender).send(any(SimpleMailMessage.class));

        book("Anna Meier", "anna@example.com");

        ReservationRecord saved = ctx.fetchSingle(RESERVATION);
        assertThat(saved.getStatus()).isEqualTo("BOOKED");
        assertThat($(Span.class).id("reservation-number").getText()).isEqualTo(saved.getReservationNumber());
    }

    // Scenario "Room taken while the guest was booking"
    @Test
    void room_taken_meanwhile_shows_message_and_results_again() {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();
        test(field("Name")).setValue("Anna Meier");
        test(field("Email")).setValue("anna@example.com");
        reservationService.create(new Stay(roomId(ctx, "104"), MARCH_11, MARCH_13, 2),
                new Guest("Ben Keller", "ben@example.com", null));
        // An edit after the search must not change the repeated search
        test($(IntegerField.class).withLabel("Guests").single()).setValue(4);
        test($(Button.class).id("book")).click();

        assertThat(message()).isEqualTo("The room is no longer available for these dates");
        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(1);
        assertThat(cards()).hasSize(6);
        assertThat($(IntegerField.class).withLabel("Guests").single().getValue()).isEqualTo(2);
        assertThat($(Button.class).withId("book").exists()).isFalse();
    }

    private void book(String name, String email) {
        navigate(BookingView.class);
        search(MARCH_10, MARCH_12, 2);
        chooseFirstRoom();
        test(field("Name")).setValue(name);
        test(field("Email")).setValue(email);
        test($(Button.class).id("book")).click();
    }

    private void search(LocalDate arrival, LocalDate departure, int guests) {
        test($(DatePicker.class).withLabel("Arrival").single()).setValue(arrival);
        test($(DatePicker.class).withLabel("Departure").single()).setValue(departure);
        test($(IntegerField.class).withLabel("Guests").single()).setValue(guests);
        test($(Button.class).id("search-rooms")).click();
    }

    private void chooseFirstRoom() {
        test($(Button.class).withClassName("choose-room").first()).click();
    }

    private List<Div> cards() {
        return $(Div.class).withClassName("room-card").all();
    }

    private List<String> cardTexts(Div card) {
        return $(Paragraph.class).from(card).all().stream().map(Paragraph::getText).toList();
    }

    private String message() {
        return $(Span.class).id("booking-message").getText();
    }

    private TextField field(String label) {
        return $(TextField.class).withLabel(label).single();
    }
}
