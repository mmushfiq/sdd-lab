package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.GuestRecord;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.email.RetryingEmailSender;
import ch.martinelli.edu.hrs.management.GuestReservation;
import ch.martinelli.edu.hrs.reservation.ReservationLookupService;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-024 "Erase Guest Data". Test data: V120__uc024_test_data.sql (Erika Erase with the closed
 * reservations ERASE00001 Checked Out, ERASE00002 Cancelled and ERASE00003 No-Show; Otto Open with the Confirmed
 * reservation ERASE00004 and the Checked Out ERASE00005); staff members from V103__uc022_test_data.sql
 * (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC024EraseGuestDataTest extends HrsBrowserlessTest {

    private static final long ERIKA = 3201;
    private static final long OTTO = 3202;
    private static final String ERIKA_EMAIL = "erika.erase@example.com";
    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    RetryingEmailSender retryingEmailSender;

    @Autowired
    ReservationLookupService lookupService;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    static EmailService emailService() {
        return EMAIL_SERVICE;
    }

    @BeforeEach
    void resetEmailService() {
        EMAIL_SERVICE.reset();
    }

    @AfterEach
    void restoreGuests() {
        SecurityContextHolder.clearContext();
        EMAIL_SERVICE.reset();
        retryingEmailSender.stopPendingEmailsTo(ERIKA_EMAIL);
        retryingEmailSender.stopPendingEmailsTo("someone.else@example.com");
        restore(ERIKA, "Erika", "Erase", ERIKA_EMAIL, "+41 79 320 32 01");
        restore(OTTO, "Otto", "Open", "otto.open@example.com", "+41 79 320 32 02");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024", businessRules = {"BR-001", "BR-002"})
    void manager_erases_the_data_of_a_guest() {
        List<ReservationRecord> reservationsBefore = erikasReservations();
        logIn();

        // Step 1
        test($(RouterLink.class).withText("Guest erasure").single()).click();
        assertThat(getCurrentView()).isInstanceOf(GuestErasureView.class);

        // Step 2: the email address, compared case-insensitively
        findGuest("Erika.Erase@Example.com");

        // Step 3: the guest and all their reservations
        assertThat(text("guest-name")).isEqualTo("Name: Erika Erase");
        assertThat(text("guest-email")).isEqualTo("Email address: erika.erase@example.com");
        assertThat(text("guest-phone")).isEqualTo("Phone number: +41 79 320 32 01");
        Grid<GuestReservation> grid = reservationGrid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Reservation code", "Arrival", "Departure", "Status");
        assertThat(rows(grid)).containsExactly(
                List.of("ERASE00001", "Apr 10, 2029", "Apr 12, 2029", "Checked Out"),
                List.of("ERASE00002", "Apr 20, 2029", "Apr 21, 2029", "Cancelled"),
                List.of("ERASE00003", "Apr 25, 2029", "Apr 26, 2029", "No-Show"));

        // Steps 4 to 6: no open reservation (BR-002), the system asks to confirm and states it cannot be undone
        test($(Button.class).withText("Erase guest data").single()).click();
        ConfirmDialog dialog = $(ConfirmDialog.class).single();
        assertThat(dialog.getElement().getProperty("message")).isEqualTo("The guest's first name, last name, email "
                + "address and phone number are replaced. The erasure cannot be undone.");

        // Step 7
        test(dialog).confirm();

        // Step 9
        assertThat(message()).isEqualTo("The guest data is erased. The guest can no longer be identified.");
        assertThat($(Grid.class).withId("guest-reservations").exists()).isFalse();

        // Step 8 / BR-001: exactly first name, last name, email address and phone number are replaced; the email
        // address is unique, has the format of an email address and cannot receive email
        GuestRecord erased = ctx.fetchSingle(GUEST, GUEST.ID.eq(ERIKA));
        assertThat(erased.getFirstName()).isNotEqualTo("Erika");
        assertThat(erased.getLastName()).isNotEqualTo("Erase");
        assertThat(erased.getPhone()).isNotEqualTo("+41 79 320 32 01");
        assertThat(erased.getEmail()).isEqualTo("guest-3201@erased.invalid");
        assertThat(ctx.fetchCount(GUEST, GUEST.EMAIL.eq(erased.getEmail()))).isEqualTo(1);

        // Success postconditions: the reservations still exist unchanged, so reports stay correct
        assertThat(erikasReservations()).isEqualTo(reservationsBefore);
        // ... they can no longer be found with the reservation code and the former email address (UC-002 BR-004)
        assertThat(lookupService.reload("ERASE00001", ERIKA_EMAIL)).isEmpty();
        // ... and the guest can no longer be found by the former email address
        findGuest(ERIKA_EMAIL);
        assertThat(message()).isEqualTo("No guest with this email address exists.");
    }

    @Test
    @UseCase(id = "UC-024")
    void pending_emails_to_the_guest_are_stopped() {
        // An email to Erika and one to someone else wait for a retry
        EMAIL_SERVICE.setRefusing(true);
        LocalDateTime now = LocalDateTime.now(clock);
        retryingEmailSender.send(new Email(ERIKA_EMAIL, "Reservation", "Text"), now);
        retryingEmailSender.send(new Email("someone.else@example.com", "Reservation", "Text"), now);
        assertThat(retryingEmailSender.hasPendingEmailsTo(ERIKA_EMAIL)).isTrue();
        openGuestErasure();
        findGuest(ERIKA_EMAIL);

        test($(Button.class).withText("Erase guest data").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        // Step 8: no email is sent to the guest any more; other emails are still retried
        assertThat(retryingEmailSender.hasPendingEmailsTo(ERIKA_EMAIL)).isFalse();
        assertThat(retryingEmailSender.hasPendingEmailsTo("someone.else@example.com")).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Guest Not Found
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024", scenario = "A1: Guest Not Found")
    void unknown_email_address_is_reported() {
        openGuestErasure();

        findGuest("nobody@example.com");

        // A1 step 1
        assertThat(message()).isEqualTo("No guest with this email address exists.");
        assertThat($(Grid.class).withId("guest-reservations").exists()).isFalse();

        // A1 step 2: the use case continues at step 2
        findGuest(ERIKA_EMAIL);
        assertThat(text("guest-name")).isEqualTo("Name: Erika Erase");
        assertThat($(Paragraph.class).withId("guest-erasure-message").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Open Reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024", scenario = "A2: Open Reservation", businessRules = {"BR-002"})
    void guest_with_an_open_reservation_cannot_be_erased() {
        openGuestErasure();
        findGuest("otto.open@example.com");
        assertThat(rows(reservationGrid())).containsExactly(
                List.of("ERASE00005", "Apr 1, 2029", "Apr 2, 2029", "Checked Out"),
                List.of("ERASE00004", "Sep 1, 2031", "Sep 3, 2031", "Confirmed"));

        test($(Button.class).withText("Erase guest data").single()).click();

        // A2 step 1: the open reservations and what to do; no confirmation is asked
        assertThat(message()).isEqualTo("The guest has open reservations: ERASE00004 (Confirmed). Cancel them or "
                + "check the guest out before the erasure.");
        assertThat($(ConfirmDialog.class).exists()).isFalse();
        // A2 step 2: the use case ends
        assertThat($(Button.class).withText("Erase guest data").single().isEnabled()).isFalse();
        // Failure postcondition: the guest data is unchanged
        GuestRecord otto = ctx.fetchSingle(GUEST, GUEST.ID.eq(OTTO));
        assertThat(otto.getFirstName()).isEqualTo("Otto");
        assertThat(otto.getEmail()).isEqualTo("otto.open@example.com");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Manager Does Not Confirm
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024", scenario = "A3: Manager Does Not Confirm", businessRules = {"BR-003"})
    void guest_data_stays_unchanged_when_the_manager_does_not_confirm() {
        openGuestErasure();
        findGuest(ERIKA_EMAIL);
        test($(Button.class).withText("Erase guest data").single()).click();

        // A3: e.g. a legal retention duty applies (BR-003)
        test($(ConfirmDialog.class).single()).cancel();

        // A3 step 1: unchanged
        GuestRecord erika = ctx.fetchSingle(GUEST, GUEST.ID.eq(ERIKA));
        assertThat(erika.getFirstName()).isEqualTo("Erika");
        assertThat(erika.getLastName()).isEqualTo("Erase");
        assertThat(erika.getEmail()).isEqualTo(ERIKA_EMAIL);
        assertThat(erika.getPhone()).isEqualTo("+41 79 320 32 01");
        assertThat($(Paragraph.class).withId("guest-erasure-message").exists()).isFalse();
        assertThat(text("guest-name")).isEqualTo("Name: Erika Erase");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Business rules
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024", businessRules = {"BR-004"})
    void former_guest_who_reserves_again_is_a_new_guest() {
        openGuestErasure();
        findGuest(ERIKA_EMAIL);
        test($(Button.class).withText("Erase guest data").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        // The former email address belongs to no guest any more, so a new reservation records a new guest
        assertThat(ctx.fetchExists(GUEST, GUEST.EMAIL.eq(ERIKA_EMAIL))).isFalse();
        assertThat(ctx.fetchSingle(GUEST, GUEST.ID.eq(ERIKA)).getEmail()).isNotEqualTo(ERIKA_EMAIL);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Preconditions
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-024")
    void other_roles_cannot_erase_guest_data() {
        // Precondition: the manager is logged in (NFR-009)
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue("rita");
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();

        assertThat($(RouterLink.class).withText("Guest erasure").exists()).isFalse();
        assertThat(navigate("guest-erasure", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void restore(long id, String firstName, String lastName, String email, String phone) {
        ctx.update(GUEST)
                .set(GUEST.FIRST_NAME, firstName)
                .set(GUEST.LAST_NAME, lastName)
                .set(GUEST.EMAIL, email)
                .set(GUEST.PHONE, phone)
                .where(GUEST.ID.eq(id))
                .execute();
    }

    private List<ReservationRecord> erikasReservations() {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.GUEST_ID.eq(ERIKA)).orderBy(RESERVATION.ID).fetch();
    }

    private void logIn() {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue("mark");
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openGuestErasure() {
        logIn();
        navigate(GuestErasureView.class);
    }

    private void findGuest(String email) {
        test($(EmailField.class).withCaption("Email address").single()).setValue(email);
        test($(Button.class).withText("Find guest").single()).click();
    }

    private String text(String id) {
        return $(Paragraph.class).withId(id).single().getText();
    }

    private String message() {
        return text("guest-erasure-message");
    }

    @SuppressWarnings("unchecked")
    private Grid<GuestReservation> reservationGrid() {
        return $(Grid.class).withId("guest-reservations").single();
    }

    private List<List<String>> rows(Grid<GuestReservation> grid) {
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> IntStream.range(0, grid.getColumns().size())
                        .mapToObj(column -> test(grid).getCellText(row, column)).toList())
                .toList();
    }
}
