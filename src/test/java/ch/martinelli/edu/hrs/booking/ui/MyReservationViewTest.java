package ch.martinelli.edu.hrs.booking.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithAnonymousUser
class MyReservationViewTest extends HrsBrowserlessTest {

    // Today is 1 March 2030, 09:00 in Zurich
    @TestBean
    Clock clock;

    static final LocalDate MARCH_1 = LocalDate.of(2030, 3, 1);
    static final LocalDate MARCH_2 = LocalDate.of(2030, 3, 2);
    static final String DEADLINE_PASSED =
            "The online cancellation deadline has passed. Please contact the hotel to cancel.";

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-03-01T08:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @Autowired
    DSLContext ctx;

    // Scenario "Reservation found"
    @Test
    void found_reservation_shows_details() {
        insert(ctx, "FOUND001", "104", MARCH_11, MARCH_13, "Anna Meier", "BOOKED");
        find(" found001 ", "Guest@Example.com");

        assertThat(details()).containsExactly("Reservation number: FOUND001", "Room type: Double",
                "Arrival: 2030-03-11", "Departure: 2030-03-13", "Guests: 1", "Guest name: Anna Meier",
                "Price per night: 2 nights × CHF 140.00", "Total price: CHF 280.00", "Status: Booked",
                "Free online cancellation until 9 March 2030, 14:00 (Zurich time)");
        assertThat(message()).isEmpty();
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isTrue();
    }

    // Scenario "Price per night shown to the guest"
    @Test
    void shows_the_price_per_night() {
        insert(ctx, "SEASON01", "104", DECEMBER_18, DECEMBER_21, "Anna Meier", "BOOKED", "140.00", "140.00", "180.00");
        find("SEASON01", "guest@example.com");

        assertThat(details()).contains("Price per night: 2 nights × CHF 140.00, 1 night × CHF 180.00",
                "Total price: CHF 460.00");
    }

    // Scenario "Wrong reservation number or email": same message, no data
    @Test
    void unknown_number_and_wrong_email_show_the_same_message() {
        insert(ctx, "FOUND001", "104", MARCH_11, MARCH_13, "Anna Meier", "BOOKED");

        find("NOPE0001", "guest@example.com");
        assertThat(message()).isEqualTo("No reservation found for this reservation number and email");
        assertThat(details()).isEmpty();

        find("FOUND001", "other@example.com");
        assertThat(message()).isEqualTo("No reservation found for this reservation number and email");
        assertThat(details()).isEmpty();
    }

    // Scenario "Guest cancels before the arrival date"
    @Test
    void cancels_after_confirmation() {
        ReservationRecord r = insert(ctx, "FOUND001", "104", MARCH_11, MARCH_13, "Anna Meier", "BOOKED");
        find("FOUND001", "guest@example.com");
        test($(Button.class).id("guest-cancel-reservation")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reload(r).getStatus()).isEqualTo("CANCELLED");
        assertThat(details()).contains("Status: Cancelled");
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isFalse();
    }

    // Scenario "Guest does not confirm"
    @Test
    void not_confirmed_keeps_reservation() {
        ReservationRecord r = insert(ctx, "FOUND001", "104", MARCH_11, MARCH_13, "Anna Meier", "BOOKED");
        find("FOUND001", "guest@example.com");
        test($(Button.class).id("guest-cancel-reservation")).click();
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(reload(r).getStatus()).isEqualTo("BOOKED");
    }

    // Scenario "Cancelled reservation found"
    @Test
    void cancelled_reservation_has_no_cancel_action() {
        insert(ctx, "CANCEL01", "104", MARCH_11, MARCH_13, "Anna Meier", "CANCELLED");
        find("CANCEL01", "guest@example.com");

        assertThat(details()).contains("Status: Cancelled");
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isFalse();
        assertThat($(Paragraph.class).withId("cancellation-deadline").exists()).isFalse();
    }

    // Scenario "Cancel on or after the arrival date"
    @Test
    void on_arrival_day_asks_to_contact_the_hotel() {
        insert(ctx, "TODAY001", "104", MARCH_1, MARCH_2, "Anna Meier", "BOOKED");
        insert(ctx, "STAYING1", "105", MARCH_1.minusDays(2), MARCH_2, "Ben Keller", "BOOKED");

        find("TODAY001", "guest@example.com");
        assertThat(message()).isEqualTo(DEADLINE_PASSED);
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isFalse();

        find("STAYING1", "guest@example.com");
        assertThat(message()).isEqualTo(DEADLINE_PASSED);
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isFalse();
    }

    // Scenario "Last cancellation time shown"
    @Test
    void shows_the_last_cancellation_time() {
        insert(ctx, "FOUND001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");
        find("FOUND001", "guest@example.com");

        assertThat($(Paragraph.class).id("cancellation-deadline").getText())
                .isEqualTo("Free online cancellation until 8 March 2030, 14:00 (Zurich time)");
    }

    // Scenario "Cancel after the deadline but before the arrival date": tomorrow's deadline was yesterday 14:00
    @Test
    void after_the_deadline_before_arrival_asks_to_contact_the_hotel() {
        insert(ctx, "TOMORR01", "104", MARCH_2, MARCH_2.plusDays(2), "Anna Meier", "BOOKED");
        find("TOMORR01", "guest@example.com");

        assertThat(message()).isEqualTo(DEADLINE_PASSED);
        assertThat($(Button.class).withId("guest-cancel-reservation").exists()).isFalse();
        assertThat($(Paragraph.class).withId("cancellation-deadline").exists()).isFalse();
    }

    // Scenario "Reservation from before the rule cancelled the day before arrival": deadline 00:00 on 2 March
    @Test
    void reservation_from_before_the_rule_is_cancelled_the_day_before_arrival() {
        ReservationRecord r = insert(ctx, "OLDRULE1", "104", MARCH_2, MARCH_2.plusDays(2), "Anna Meier", "BOOKED");
        ctx.update(RESERVATION).set(RESERVATION.CANCELLATION_HOURS_BEFORE, 14).where(RESERVATION.ID.eq(r.getId()))
                .execute();
        find("OLDRULE1", "guest@example.com");

        assertThat($(Paragraph.class).id("cancellation-deadline").getText())
                .isEqualTo("Free online cancellation until 2 March 2030, 00:00 (Zurich time)");
        test($(Button.class).id("guest-cancel-reservation")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reload(r).getStatus()).isEqualTo("CANCELLED");
    }

    // Cancel acts on the reservation shown, even if the guest edits the fields after finding it
    @Test
    void cancel_uses_the_reservation_shown_not_edited_fields() {
        ReservationRecord shown = insert(ctx, "FOUND001", "104", MARCH_11, MARCH_13, "Anna Meier", "BOOKED");
        ReservationRecord other = insert(ctx, "OTHER001", "105", MARCH_11, MARCH_13, "Ben Keller", "BOOKED");
        find("FOUND001", "guest@example.com");
        test($(TextField.class).withLabel("Reservation number").single()).setValue("OTHER001");
        test($(Button.class).id("guest-cancel-reservation")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reload(shown).getStatus()).isEqualTo("CANCELLED");
        assertThat(reload(other).getStatus()).isEqualTo("BOOKED");
    }

    private void find(String number, String email) {
        navigate(MyReservationView.class);
        test($(TextField.class).withLabel("Reservation number").single()).setValue(number);
        test($(TextField.class).withLabel("Email").single()).setValue(email);
        test($(Button.class).id("find-reservation")).click();
    }

    private List<String> details() {
        return $(Paragraph.class).from($(Div.class).id("my-reservation-details")).all().stream()
                .map(Paragraph::getText).toList();
    }

    private String message() {
        return $(Span.class).id("my-reservation-message").getText();
    }

    private ReservationRecord reload(ReservationRecord r) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(r.getId()));
    }
}
