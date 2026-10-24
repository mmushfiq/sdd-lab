package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reservation.ReceptionCancellationService;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.reservation.ReceptionRoomType;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-006 "Cancel Reservation". Test data: V107__uc006_test_data.sql, room types from
 * V100__uc001_test_data.sql and V106__uc005_test_data.sql, staff members from V103__uc022_test_data.sql (password
 * Secret-123); now is 1 June 2030, 12:00 hotel time.
 */
@WithAnonymousUser
class UC006CancelReservationTest extends HrsBrowserlessTest {

    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    ReceptionCancellationService cancellationService;

    @Autowired
    ReceptionReservationService receptionReservationService;

    @Autowired
    DSLContext ctx;

    private Result<ReservationRecord> originalReservations;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    static EmailService emailService() {
        return EMAIL_SERVICE;
    }

    @BeforeEach
    void rememberTestData() {
        EMAIL_SERVICE.reset();
        originalReservations = ctx.selectFrom(RESERVATION).where(RESERVATION.ID.between(1901L, 1999L)).fetch();
    }

    @AfterEach
    void restoreTestData() {
        EMAIL_SERVICE.reset();
        SecurityContextHolder.clearContext();
        originalReservations.forEach(original -> {
            original.changed(true);
            original.changed(RESERVATION.ID, false);
            ctx.update(RESERVATION).set(original).where(RESERVATION.ID.eq(original.getId())).execute();
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006", businessRules = {"BR-002", "BR-003"})
    void receptionist_cancels_a_confirmed_reservation() {
        long availableBefore = availableDoubleRooms();
        logIn("rita");

        // Step 1: the reservation found with UC-007
        navigate(SearchReservationsView.class);
        test($(TextField.class).withCaption("Reservation code").single()).setValue("CANCELR001");
        test($(Button.class).withText("Search").single()).click();
        test($(Grid.class).single()).select(0);

        // Step 2: status, guest, dates, room type and total price
        assertThat(field("status")).isEqualTo("Status: Confirmed");
        assertThat(field("guest-name")).isEqualTo("Name: Rosa Storno");
        assertThat(field("arrival-date")).isEqualTo("Arrival date: Mar 20, 2031");
        assertThat(field("departure-date")).isEqualTo("Departure date: Mar 22, 2031");
        assertThat(field("room-type")).isEqualTo("Room type: Double Room");
        assertThat(field("total-price")).isEqualTo("Total price: CHF 300.00");

        // Step 3
        requestCancellation();

        // Step 5: confirmation that states it cannot be undone
        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertThat(dialog.isOpened()).isTrue();
        assertThat(dialog.getElement().getProperty("message")).isEqualTo("The cancellation cannot be undone.");

        // Step 6
        test(dialog).confirm();

        // Step 9: the reservation is displayed as cancelled
        assertThat(field("reservation-message")).isEqualTo(ReservationView.CANCELLED);
        assertThat(field("status")).isEqualTo("Status: Cancelled");
        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();

        // Step 7: status Cancelled and the time of cancellation; BR-002: the price is unchanged, no fee
        ReservationRecord reservation = reservation("CANCELR001");
        assertThat(reservation.getStatus()).isEqualTo("CANCELLED");
        assertThat(reservation.getCancelledAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo("300.00");

        // Success postcondition: the room type is available again for its nights
        assertThat(availableDoubleRooms()).isEqualTo(availableBefore + 1);

        // Step 8: cancellation email with reservation code, dates and room type
        assertThat(EMAIL_SERVICE.sentEmails()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("rosa.storno@example.com");
            assertThat(email.body()).contains("Reservation code: CANCELR001", "Arrival date: 2031-03-20",
                    "Departure date: 2031-03-22", "Room type: Double Room");
        });
    }

    @Test
    @UseCase(id = "UC-006", businessRules = {"BR-001"})
    void reservation_can_be_cancelled_on_the_arrival_date_after_the_online_deadline() {
        openReservation("CANCELR002");

        requestCancellation();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reservation("CANCELR002").getStatus()).isEqualTo("CANCELLED");
        assertThat(field("status")).isEqualTo("Status: Cancelled");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Reservation not confirmed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006", scenario = "A1: Reservation Not Confirmed", businessRules = {"BR-001"})
    void reservation_without_status_confirmed_offers_no_cancellation() {
        openReservation("CANCELR003");

        assertThat(field("status")).isEqualTo("Status: No-Show");
        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-006", scenario = "A1: Reservation Not Confirmed", businessRules = {"BR-001"})
    void receptionist_is_informed_when_the_status_does_not_allow_the_cancellation() {
        openReservation("CANCELR001");

        // The displayed reservation is out of date: meanwhile a no-show was recorded
        ctx.update(RESERVATION).set(RESERVATION.STATUS, "NO_SHOW")
                .where(RESERVATION.RESERVATION_CODE.eq("CANCELR001")).execute();
        requestCancellation();

        // A1 step 1: informed, no confirmation is asked
        assertThat(field("reservation-message")).isEqualTo("A reservation with status No-Show cannot be cancelled.");
        assertThat($(ConfirmDialog.class).exists()).isFalse();
        assertThat(reservation("CANCELR001").getStatus()).isEqualTo("NO_SHOW");
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Receptionist keeps the reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006", scenario = "A2: Receptionist Keeps the Reservation")
    void reservation_stays_unchanged_when_the_receptionist_does_not_confirm() {
        ReservationRecord before = reservation("CANCELR001");
        openReservation("CANCELR001");

        requestCancellation();
        test($(ConfirmDialog.class).single()).cancel();

        // A2 step 1: unchanged and displayed again
        assertThat(reservation("CANCELR001")).isEqualTo(before);
        assertThat(field("status")).isEqualTo("Status: Confirmed");
        assertThat($(Button.class).withText("Cancel reservation").exists()).isTrue();
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Status changed before confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006", scenario = "A3: Status Changed Before Confirmation")
    void cancellation_is_refused_when_someone_else_cancelled_the_reservation_meanwhile() {
        openReservation("CANCELR001");
        requestCancellation();

        // Another receptionist cancels the same reservation before the confirmation
        assertThat(cancellationService.cancel("CANCELR001")).isTrue();
        LocalDateTime firstCancellation = reservation("CANCELR001").getCancelledAt();

        test($(ConfirmDialog.class).single()).confirm();

        // A3 step 1: refused, the current status is displayed
        assertThat(field("reservation-message")).isEqualTo("A reservation with status Cancelled cannot be cancelled.");
        assertThat(field("status")).isEqualTo("Status: Cancelled");
        assertThat(reservation("CANCELR001").getCancelledAt()).isEqualTo(firstCancellation);
        // Only the other receptionist's cancellation sent an email
        assertThat(EMAIL_SERVICE.sentEmails()).hasSize(1);
    }

    @Test
    @UseCase(id = "UC-006", scenario = "A3: Status Changed Before Confirmation")
    void cancellation_is_refused_when_a_no_show_was_recorded_meanwhile() {
        ReservationRecord before = reservation("CANCELR001");
        openReservation("CANCELR001");
        requestCancellation();

        ctx.update(RESERVATION).set(RESERVATION.STATUS, "NO_SHOW")
                .where(RESERVATION.RESERVATION_CODE.eq("CANCELR001")).execute();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(field("reservation-message")).isEqualTo("A reservation with status No-Show cannot be cancelled.");
        assertThat(field("status")).isEqualTo("Status: No-Show");
        // Failure postcondition: the reservation keeps its data
        ReservationRecord after = reservation("CANCELR001");
        assertThat(after.getCancelledAt()).isNull();
        assertThat(after.getDepartureDate()).isEqualTo(before.getDepartureDate());
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Email Service does not accept the cancellation email
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006", scenario = "A4: Email Service Does Not Accept the Cancellation Email",
            businessRules = {"BR-003"})
    void reservation_stays_cancelled_when_the_email_service_refuses_the_email() {
        EMAIL_SERVICE.setRefusing(true);
        openReservation("CANCELR001");

        requestCancellation();
        test($(ConfirmDialog.class).single()).confirm();

        // A4 step 2: continues at step 9
        assertThat(field("reservation-message")).isEqualTo(ReservationView.CANCELLED);
        // A4 step 1: the reservation stays cancelled
        assertThat(reservation("CANCELR001").getStatus()).isEqualTo("CANCELLED");
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-006")
    void only_a_logged_in_receptionist_can_cancel_a_reservation() {
        navigate("reservation-details/CANCELR001", LoginView.class);

        logIn("mark");
        assertThat(navigate("reservation-details/CANCELR001", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openReservation(String reservationCode) {
        logIn("rita");
        navigate(ReservationView.class, reservationCode);
    }

    private void requestCancellation() {
        test($(Button.class).withText("Cancel reservation").single()).click();
    }

    private String field(String id) {
        return $(Paragraph.class).withId(id).single().getText();
    }

    private long availableDoubleRooms() {
        return receptionReservationService.searchAvailableRoomTypes(LocalDate.of(2031, 3, 20),
                        LocalDate.of(2031, 3, 22), 1).stream()
                .filter(roomType -> roomType.name().equals("Double Room"))
                .mapToLong(ReceptionRoomType::availableRooms)
                .findFirst()
                .orElse(0);
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }
}
