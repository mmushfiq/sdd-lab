package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.MutableClock;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reservation.OnlineReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationLookupLock;
import ch.martinelli.edu.hrs.reservation.ResettableReservationLookupLock;
import ch.martinelli.edu.hrs.reservation.AvailableRoomType;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-003 "Cancel Own Reservation". Test data: V102__uc003_test_data.sql; the tests start on
 * 1 June 2030 at 12:00 hotel time.
 */
class UC003CancelOwnReservationTest extends HrsBrowserlessTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Europe/Zurich");
    private static final Instant START = Instant.parse("2030-06-01T10:00:00Z");
    private static final MutableClock CLOCK = new MutableClock(START, HOTEL_ZONE);
    private static final ResettableReservationLookupLock LOOKUP_LOCK = new ResettableReservationLookupLock(CLOCK);
    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();
    private static final String EMAIL = "emil.cancel@example.com";

    @TestBean
    Clock clock;

    @TestBean
    ReservationLookupLock reservationLookupLock;

    @TestBean
    EmailService emailService;

    @Autowired
    OnlineReservationService reservationService;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return CLOCK;
    }

    static ReservationLookupLock reservationLookupLock() {
        return LOOKUP_LOCK;
    }

    static EmailService emailService() {
        return EMAIL_SERVICE;
    }

    @BeforeEach
    void reset() {
        CLOCK.setInstant(START);
        LOOKUP_LOCK.reset();
        EMAIL_SERVICE.reset();
    }

    @AfterEach
    void restoreTestData() {
        EMAIL_SERVICE.reset();
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, "CONFIRMED")
                .setNull(RESERVATION.CANCELLED_AT)
                .where(RESERVATION.ID.in(1201L, 1202L))
                .execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-003", "BR-004"})
    void guest_cancels_reservation_and_receives_cancellation_email() {
        Component view = openView();
        lookUp(view, "CANCEL0001", EMAIL);

        // Step 4
        assertThat(paragraphTexts(view)).contains("Reservation code: CANCEL0001", "Status: Confirmed",
                "Arrival date: Nov 20, 2030", "Departure date: Nov 22, 2030", "Room type: Family Room",
                "Number of guests: 3", "Total price: CHF 500.00");

        // Steps 5 to 7: the guest is asked to confirm and told that it cannot be undone
        requestCancellation(view);
        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertThat(dialog.isOpened()).isTrue();
        assertThat(dialog.getElement().getProperty("message")).isEqualTo("The cancellation cannot be undone.");

        // Step 8
        test(dialog).confirm();

        // Step 9: status Cancelled and time of cancellation
        ReservationRecord reservation = reservation(1201L);
        assertThat(reservation.getStatus()).isEqualTo("CANCELLED");
        assertThat(reservation.getCancelledAt()).isEqualTo(LocalDateTime.of(2030, 6, 1, 12, 0));

        // Step 10: cancellation email with reservation code, dates and room type
        List<Email> emails = EMAIL_SERVICE.sentEmails();
        assertThat(emails).hasSize(1);
        Email email = emails.getFirst();
        assertThat(email.to()).isEqualTo(EMAIL);
        assertThat(email.body()).contains("CANCEL0001", "2030-11-20", "2030-11-22", "Family Room");

        // Step 11
        assertThat($(Paragraph.class, view).withId("cancelled-message").single().getText())
                .isEqualTo(ViewOwnReservationView.CANCELLED);
        assertThat(paragraphTexts(view)).contains("Status: Cancelled");
        assertThat(cancelOffered(view)).isFalse();
    }

    @Test
    @UseCase(id = "UC-003")
    void cancellation_increases_the_availability_of_the_room_type() {
        LocalDate arrival = LocalDate.of(2030, 11, 20);
        LocalDate departure = LocalDate.of(2030, 11, 22);
        // Only the Family Room fits 3 guests, and its only room is reserved by CANCEL0001
        assertThat(availableRoomTypes(arrival, departure, 3)).isEmpty();

        Component view = openView();
        lookUp(view, "CANCEL0001", EMAIL);
        requestCancellation(view);
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(availableRoomTypes(arrival, departure, 3)).containsExactly("Family Room");
    }

    @Test
    @UseCase(id = "UC-003", businessRules = {"BR-001"})
    void cancellation_is_possible_one_minute_before_the_deadline() {
        // Arrival 2 June: the last online cancellation is on 1 June at 13:59
        CLOCK.setInstant(LocalDateTime.of(2030, 6, 1, 13, 59).atZone(HOTEL_ZONE).toInstant());
        Component view = openView();
        lookUp(view, "CANCEL0002", EMAIL);

        requestCancellation(view);
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reservation(1202L).getStatus()).isEqualTo("CANCELLED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1 to A3: lookup (UC-002 rules)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A1: Lookup Input Missing")
    void missing_input_is_marked() {
        Component view = openView();

        lookUp(view, "", "");

        assertThat($(TextField.class, view).withCaption("Reservation code").single().isInvalid()).isTrue();
        assertThat($(EmailField.class, view).withCaption("Email address").single().isInvalid()).isTrue();
        assertThat(cancelOffered(view)).isFalse();
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A2: Reservation Not Found", businessRules = {"BR-005"})
    void reservation_of_another_guest_cannot_be_cancelled() {
        Component view = openView();

        // LOOKUP0001 belongs to Carla Lookup
        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(ViewOwnReservationView.NOT_FOUND);
        assertThat(cancelOffered(view)).isFalse();
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A3: Lookup Locked", businessRules = {"BR-005"})
    void locked_lookup_refuses_the_cancellation() {
        Component view = openView();
        for (int attempt = 1; attempt <= 5; attempt++) {
            lookUp(view, "WRONG0000" + attempt, EMAIL);
        }

        lookUp(view, "CANCEL0001", EMAIL);

        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(ViewOwnReservationView.LOCKED);
        assertThat(cancelOffered(view)).isFalse();
        assertThat(reservation(1201L).getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Reservation Not Confirmed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A4: Reservation Not Confirmed", businessRules = {"BR-002"})
    void cancelled_reservation_cannot_be_cancelled() {
        Component view = openView();

        lookUp(view, "CANCEL0003", EMAIL);

        assertThat(cancelOffered(view)).isFalse();
        assertThat(notCancellableMessage(view)).isEqualTo("A reservation with status Cancelled cannot be cancelled.");
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A4: Reservation Not Confirmed", businessRules = {"BR-002"})
    void checked_out_and_no_show_reservations_cannot_be_cancelled() {
        Component view = openView();

        lookUp(view, "LOOKUP0005", "carla.lookup@example.com");
        assertThat(cancelOffered(view)).isFalse();
        assertThat(notCancellableMessage(view))
                .isEqualTo("A reservation with status Checked Out cannot be cancelled.");

        lookUp(view, "LOOKUP0006", "carla.lookup@example.com");
        assertThat(cancelOffered(view)).isFalse();
        assertThat(notCancellableMessage(view)).isEqualTo("A reservation with status No-Show cannot be cancelled.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Cancellation Deadline Passed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A5: Cancellation Deadline Passed", businessRules = {"BR-001"})
    void cancellation_is_refused_from_the_deadline_on() {
        // Arrival 2 June: from 1 June 14:00 only the reception can cancel
        CLOCK.setInstant(LocalDateTime.of(2030, 6, 1, 14, 0).atZone(HOTEL_ZONE).toInstant());
        Component view = openView();

        lookUp(view, "CANCEL0002", EMAIL);

        assertThat(cancelOffered(view)).isFalse();
        assertThat(notCancellableMessage(view)).isEqualTo(ViewOwnReservationView.DEADLINE_PASSED);
        assertThat(reservation(1202L).getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A6: Guest Keeps the Reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A6: Guest Keeps the Reservation")
    void guest_keeps_the_reservation() {
        Component view = openView();
        lookUp(view, "CANCEL0001", EMAIL);
        requestCancellation(view);

        test($(ConfirmDialog.class).single()).cancel();

        ReservationRecord reservation = reservation(1201L);
        assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reservation.getCancelledAt()).isNull();
        assertThat(paragraphTexts(view)).contains("Reservation code: CANCEL0001", "Status: Confirmed");
        assertThat(cancelOffered(view)).isTrue();
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A7: Deadline Passes Before Confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A7: Deadline Passes Before Confirmation", businessRules = {"BR-001"})
    void deadline_passes_while_the_guest_confirms() {
        CLOCK.setInstant(LocalDateTime.of(2030, 6, 1, 13, 59).atZone(HOTEL_ZONE).toInstant());
        Component view = openView();
        lookUp(view, "CANCEL0002", EMAIL);
        requestCancellation(view);

        CLOCK.setInstant(LocalDateTime.of(2030, 6, 1, 14, 0).atZone(HOTEL_ZONE).toInstant());
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reservation(1202L).getStatus()).isEqualTo("CONFIRMED");
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
        // Continues at step 6
        assertThat(cancelOffered(view)).isFalse();
        assertThat(notCancellableMessage(view)).isEqualTo(ViewOwnReservationView.DEADLINE_PASSED);
    }

    @Test
    @UseCase(id = "UC-003", scenario = "A7: Deadline Passes Before Confirmation", businessRules = {"BR-002"})
    void status_changes_while_the_guest_confirms() {
        Component view = openView();
        lookUp(view, "CANCEL0001", EMAIL);
        requestCancellation(view);

        // Meanwhile a receptionist cancels the reservation (UC-006)
        LocalDateTime receptionCancelledAt = LocalDateTime.of(2030, 6, 1, 11, 30);
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, "CANCELLED")
                .set(RESERVATION.CANCELLED_AT, receptionCancelledAt)
                .where(RESERVATION.ID.eq(1201L))
                .execute();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reservation(1201L).getCancelledAt()).isEqualTo(receptionCancelledAt);
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
        assertThat(paragraphTexts(view)).contains("Status: Cancelled");
        assertThat(notCancellableMessage(view)).isEqualTo("A reservation with status Cancelled cannot be cancelled.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A8: Email Service Does Not Accept the Cancellation Email
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-003", scenario = "A8: Email Service Does Not Accept the Cancellation Email",
            businessRules = {"BR-004"})
    void reservation_stays_cancelled_when_the_email_service_refuses_the_email() {
        EMAIL_SERVICE.setRefusing(true);
        Component view = openView();
        lookUp(view, "CANCEL0001", EMAIL);
        requestCancellation(view);

        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reservation(1201L).getStatus()).isEqualTo("CANCELLED");
        assertThat($(Paragraph.class, view).withId("cancelled-message").single().getText())
                .isEqualTo(ViewOwnReservationView.CANCELLED_EMAIL_DELAYED);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private Component openView() {
        return navigate(ViewOwnReservationView.class);
    }

    private void lookUp(Component view, String reservationCode, String email) {
        test($(TextField.class, view).withCaption("Reservation code").single()).setValue(reservationCode);
        test($(EmailField.class, view).withCaption("Email address").single()).setValue(email);
        test($(Button.class, view).withText("Show reservation").single()).click();
    }

    private void requestCancellation(Component view) {
        test($(Button.class, view).withId("cancel-reservation").single()).click();
    }

    private boolean cancelOffered(Component view) {
        return $(Button.class, view).withId("cancel-reservation").exists();
    }

    private String notCancellableMessage(Component view) {
        return $(Paragraph.class, view).withId("not-cancellable").single().getText();
    }

    private ReservationRecord reservation(long id) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(id));
    }

    private List<String> availableRoomTypes(LocalDate arrival, LocalDate departure, int numberOfGuests) {
        return reservationService.searchAvailableRoomTypes(arrival, departure, numberOfGuests).stream()
                .map(AvailableRoomType::name)
                .toList();
    }

    private List<String> paragraphTexts(Component view) {
        return $(Paragraph.class, view).all().stream().map(Paragraph::getText).toList();
    }
}
