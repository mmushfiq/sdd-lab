package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.MutableClock;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reception.MissedArrival;
import ch.martinelli.edu.hrs.reception.NoShowService;
import ch.martinelli.edu.hrs.reservation.RoomTypeAvailabilityRepository;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
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
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-012 "Record No-Show". Test data: V112__uc012_test_data.sql (room type Igloo with the single
 * room B01) and the Loft from V108__uc008_test_data.sql; staff members from V103__uc022_test_data.sql (password
 * Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC012RecordNoShowTest extends HrsBrowserlessTest {

    private static final Instant NOW = Instant.parse("2030-06-01T10:00:00Z");
    private static final MutableClock CLOCK = new MutableClock(NOW, ZoneId.of("Europe/Zurich"));
    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    NoShowService noShowService;

    @Autowired
    RoomTypeAvailabilityRepository availabilityRepository;

    @Autowired
    DSLContext ctx;

    private Result<ReservationRecord> originalReservations;

    static Clock clock() {
        return CLOCK;
    }

    static EmailService emailService() {
        return EMAIL_SERVICE;
    }

    @BeforeEach
    void rememberTestData() {
        CLOCK.setInstant(NOW);
        EMAIL_SERVICE.reset();
        originalReservations = ctx.selectFrom(RESERVATION).where(RESERVATION.ID.between(2401L, 2499L)).fetch();
    }

    @AfterEach
    void restoreTestData() {
        CLOCK.setInstant(NOW);
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
    @UseCase(id = "UC-012", businessRules = {"BR-001", "BR-002", "BR-003"})
    void receptionist_records_a_no_show_for_a_missed_arrival() {
        // Before: the only Igloo is reserved for the nights of 1 and 2 June
        assertThat(availabilityRepository.findUnavailableNights(2401L, LocalDate.of(2030, 6, 1),
                LocalDate.of(2030, 6, 3), -1L)).hasSize(2);
        ReservationRecord before = reservation("MISSED0001");
        logIn("rita");

        // Step 1
        test($(RouterLink.class).withText("Missed arrivals").single()).click();
        assertThat(getCurrentView()).isInstanceOf(MissedArrivalsView.class);

        // Step 2: Confirmed with an arrival date before today; MISSED0003 arrives today, MISSED0004 is cancelled
        Grid<MissedArrival> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText).containsExactly(
                "Reservation code", "Guest", "Phone number", "Arrival", "Departure", "Room type");
        assertThat(rows(grid)).containsExactly(
                List.of("MISSED0001", "Nora Nichtda", "+41 79 240 24 01", "May 30, 2030", "Jun 3, 2030", "Igloo"),
                List.of("MISSED0002", "Emil Ehemals", "+41 79 240 24 02", "May 31, 2030", "Jun 1, 2030", "Loft"));

        // Steps 3 and 4
        test(grid).select(0);
        test($(Button.class).withText("Record no-show").single()).click();

        // Step 5: confirmation that states the reservation cannot be checked in afterwards
        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertThat(dialog.isOpened()).isTrue();
        assertThat(dialog.getElement().getProperty("message")).isEqualTo(MissedArrivalsView.CANNOT_BE_CHECKED_IN);

        // Step 6
        test(dialog).confirm();

        // Step 9
        assertThat(field("no-show-message")).isEqualTo("Reservation MISSED0001 is recorded as a no-show.");
        assertThat(rows(grid())).extracting(List::getFirst).containsExactly("MISSED0002");

        // Step 8 / success postconditions: status No-Show, dates, room type and total price are kept
        ReservationRecord after = reservation("MISSED0001");
        assertThat(after.getStatus()).isEqualTo("NO_SHOW");
        assertThat(after.getArrivalDate()).isEqualTo(before.getArrivalDate());
        assertThat(after.getDepartureDate()).isEqualTo(before.getDepartureDate());
        assertThat(after.getRoomTypeId()).isEqualTo(before.getRoomTypeId());
        assertThat(after.getTotalPrice()).isEqualByComparingTo(before.getTotalPrice());
        // The room type is available again for its remaining nights
        assertThat(availabilityRepository.findUnavailableNights(2401L, LocalDate.of(2030, 6, 1),
                LocalDate.of(2030, 6, 3), -1L)).isEmpty();
        // BR-003: no email to the guest
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-012", businessRules = {"BR-004"})
    void no_show_is_final() {
        openMissedArrivals();
        test(grid()).select(0);
        test($(Button.class).withText("Record no-show").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        // The reservation offers no action any more
        navigate(ReservationView.class, "MISSED0001");
        assertThat(field("status")).isEqualTo("Status: No-Show");
        assertThat($(Button.class).withText("Check in").exists()).isFalse();
        assertThat($(Button.class).withText("Change").exists()).isFalse();
        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
        // and refuses check-in and change when opened directly
        navigate(CheckInView.class, "MISSED0001");
        assertThat(field("error-message")).isEqualTo("A reservation with status No-Show cannot be checked in.");
        navigate(ChangeReservationView.class, "MISSED0001");
        assertThat(field("error-message")).isEqualTo("A reservation with status No-Show cannot be changed.");
    }

    @Test
    @UseCase(id = "UC-012", businessRules = {"BR-001"})
    void reservation_arriving_today_is_no_missed_arrival_until_the_day_has_passed() {
        openMissedArrivals();
        assertThat(rows(grid())).extracting(List::getFirst).doesNotContain("MISSED0003");

        // The next day the end of the arrival date has passed
        CLOCK.setInstant(Instant.parse("2030-06-01T22:30:00Z"));
        navigate(ArrivalsAndDeparturesView.class);
        navigate(MissedArrivalsView.class);

        assertThat(rows(grid())).extracting(List::getFirst).contains("MISSED0003");
    }

    @Test
    @UseCase(id = "UC-012")
    void missed_arrival_opened_from_the_reservation_leads_to_the_missed_arrivals() {
        logIn("rita");
        navigate(ReservationView.class, "MISSED0001");

        test($(Button.class).withText("Record no-show").single()).click();

        assertThat(getCurrentView()).isInstanceOf(MissedArrivalsView.class);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: No missed arrivals
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-012", scenario = "A1: No Missed Arrivals")
    void receptionist_is_informed_when_there_are_no_missed_arrivals() {
        // On 25 May 2030 no Confirmed reservation has an earlier arrival date
        CLOCK.setInstant(Instant.parse("2030-05-25T10:00:00Z"));

        openMissedArrivals();

        assertThat(field("no-missed-arrivals")).isEqualTo(MissedArrivalsView.NO_MISSED_ARRIVALS);
        assertThat($(Grid.class).exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Guest still expected
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-012", scenario = "A2: Guest Still Expected")
    void receptionist_changes_the_arrival_date_of_a_guest_who_is_still_coming() {
        openMissedArrivals();
        test(grid()).select(0);

        test($(Button.class).withText("Change reservation").single()).click();

        // A2 step 1: UC-005 Change Reservation
        assertThat(getCurrentView()).isInstanceOf(ChangeReservationView.class);
        assertThat(field("status")).isEqualTo("Status: Confirmed");
        assertThat(reservation("MISSED0001").getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Receptionist does not confirm
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-012", scenario = "A3: Receptionist Does Not Confirm")
    void reservation_stays_unchanged_when_the_receptionist_does_not_confirm() {
        ReservationRecord before = reservation("MISSED0001");
        openMissedArrivals();
        test(grid()).select(0);
        test($(Button.class).withText("Record no-show").single()).click();

        test($(ConfirmDialog.class).single()).cancel();

        assertThat(reservation("MISSED0001")).isEqualTo(before);
        // A3 step 2: continues at step 2
        assertThat(rows(grid())).extracting(List::getFirst).containsExactly("MISSED0001", "MISSED0002");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Status changed before confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-012", scenario = "A4: Status Changed Before Confirmation")
    void no_show_is_refused_when_someone_else_cancelled_the_reservation_meanwhile() {
        openMissedArrivals();
        test(grid()).select(0);
        test($(Button.class).withText("Record no-show").single()).click();

        // Another receptionist cancels the reservation before the confirmation
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, "CANCELLED")
                .set(RESERVATION.CANCELLED_AT, LocalDateTime.of(2030, 6, 1, 11, 0))
                .where(RESERVATION.RESERVATION_CODE.eq("MISSED0001")).execute();
        test($(ConfirmDialog.class).single()).confirm();

        // A4 step 1: refused, the current status is displayed
        assertThat(field("no-show-message"))
                .isEqualTo("A no-show cannot be recorded for reservation MISSED0001. Its status is Cancelled.");
        assertThat(reservation("MISSED0001").getStatus()).isEqualTo("CANCELLED");
        // A4 step 2: continues at step 2
        assertThat(rows(grid())).extracting(List::getFirst).containsExactly("MISSED0002");
    }

    @Test
    @UseCase(id = "UC-012", scenario = "A4: Status Changed Before Confirmation", businessRules = {"BR-001"})
    void no_show_is_refused_when_the_arrival_date_was_changed_meanwhile() {
        openMissedArrivals();
        test(grid()).select(0);
        test($(Button.class).withText("Record no-show").single()).click();

        // Another receptionist moves the arrival to today (UC-005)
        ctx.update(RESERVATION).set(RESERVATION.ARRIVAL_DATE, LocalDate.of(2030, 6, 1))
                .where(RESERVATION.RESERVATION_CODE.eq("MISSED0001")).execute();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(field("no-show-message"))
                .isEqualTo("A no-show cannot be recorded for reservation MISSED0001. Its status is Confirmed.");
        assertThat(reservation("MISSED0001").getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-012")
    void only_a_logged_in_receptionist_can_record_a_no_show() {
        navigate("missed-arrivals", LoginView.class);

        logIn("mark");
        assertThat(navigate("missed-arrivals", RouteNotFoundError.class)).isNotNull();
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

    private void openMissedArrivals() {
        logIn("rita");
        navigate(MissedArrivalsView.class);
    }

    @SuppressWarnings("unchecked")
    private Grid<MissedArrival> grid() {
        return $(Grid.class).withId("missed-arrivals").single();
    }

    private List<List<String>> rows(Grid<?> grid) {
        int columns = grid.getColumns().size();
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> IntStream.range(0, columns).mapToObj(column -> test(grid).getCellText(row, column))
                        .toList())
                .toList();
    }

    private String field(String id) {
        return $(Paragraph.class).withId(id).single().getText();
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }
}
