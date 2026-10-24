package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.ReservationLookupLock;
import ch.martinelli.edu.hrs.reservation.ResettableReservationLookupLock;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
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
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-002 "View Own Reservation". Test data: V101__uc002_test_data.sql; now is 1 June 2030, 12:00
 * hotel time.
 */
class UC002ViewOwnReservationTest extends HrsBrowserlessTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final ResettableReservationLookupLock LOOKUP_LOCK = new ResettableReservationLookupLock(CLOCK);
    private static final String EMAIL = "carla.lookup@example.com";

    @TestBean
    Clock clock;

    @TestBean
    ReservationLookupLock reservationLookupLock;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return CLOCK;
    }

    static ReservationLookupLock reservationLookupLock() {
        return LOOKUP_LOCK;
    }

    @BeforeEach
    @AfterEach
    void resetLookupLock() {
        LOOKUP_LOCK.reset();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-001"})
    void guest_sees_reservation_with_code_status_dates_nights_room_type_guests_and_total_price() {
        List<ReservationRecord> before = ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch();
        Component view = openView();

        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat($(H2.class, view).withText("Your reservation").exists()).isTrue();
        assertThat(paragraphTexts(view)).containsSequence(
                "Reservation code: LOOKUP0001",
                "Status: Confirmed",
                "Arrival date: Nov 1, 2030",
                "Departure date: Nov 4, 2030",
                "Number of nights: 3",
                "Room type: Double Room",
                "Number of guests: 2",
                "Total price: CHF 450.00");
        assertThat($(Paragraph.class, view).withId("error-message").exists()).isFalse();
        // Success postcondition: no reservation data is changed
        assertThat(ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch()).isEqualTo(before);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-001"})
    void email_address_is_compared_case_insensitively() {
        Component view = openView();

        lookUp(view, "LOOKUP0001", "Carla.Lookup@EXAMPLE.com");

        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-001"})
    void reservations_of_every_status_are_shown() {
        Component view = openView();

        lookUp(view, "LOOKUP0004", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0004", "Status: Cancelled");

        lookUp(view, "LOOKUP0005", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0005", "Status: Checked Out")
                .doesNotContain("Reservation code: LOOKUP0004");

        lookUp(view, "LOOKUP0006", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0006", "Status: No-Show");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-001"})
    void reservation_code_must_match_exactly() {
        Component view = openView();

        lookUp(view, "lookup0001", EMAIL);

        assertNotFound(view);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Lookup Input Missing
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Lookup Input Missing")
    void missing_reservation_code_is_marked() {
        Component view = openView();

        lookUp(view, "", EMAIL);

        TextField code = $(TextField.class, view).withCaption("Reservation code").single();
        assertThat(code.isInvalid()).isTrue();
        assertThat(code.getErrorMessage()).isEqualTo("Enter the reservation code");
        assertNothingShown(view);
        // A1 is not a failed lookup attempt: five more missing inputs do not lock the lookup
        for (int attempt = 1; attempt <= 5; attempt++) {
            lookUp(view, "", EMAIL);
        }
        lookUp(view, "LOOKUP0001", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Lookup Input Missing")
    void missing_email_address_is_marked_and_guest_can_continue() {
        Component view = openView();

        lookUp(view, "LOOKUP0001", "");

        EmailField email = $(EmailField.class, view).withCaption("Email address").single();
        assertThat(email.isInvalid()).isTrue();
        assertThat(email.getErrorMessage()).isEqualTo("Enter your email address");
        assertNothingShown(view);

        // Use case continues at step 2
        lookUp(view, "LOOKUP0001", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Reservation Not Found
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Reservation Not Found", businessRules = {"BR-002"})
    void wrong_code_wrong_email_or_both_give_the_same_answer() {
        Component view = openView();

        lookUp(view, "NOSUCHCODE", EMAIL);
        String wrongCode = errorMessage(view);
        lookUp(view, "LOOKUP0001", "someone.else@example.com");
        String wrongEmail = errorMessage(view);
        lookUp(view, "NOSUCHCODE", "someone.else@example.com");
        String bothWrong = errorMessage(view);

        assertThat(wrongCode).isEqualTo(ViewOwnReservationView.NOT_FOUND);
        assertThat(wrongEmail).isEqualTo(wrongCode);
        assertThat(bothWrong).isEqualTo(wrongCode);
        assertThat($(H2.class, view).withText("Your reservation").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Reservation Not Found")
    void reservation_of_another_guest_is_not_shown() {
        Component view = openView();

        // TESTSGL001 belongs to Anna Muster
        lookUp(view, "TESTSGL001", EMAIL);

        assertNotFound(view);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Reservation Not Found")
    void guest_can_try_again_after_a_failed_lookup() {
        Component view = openView();
        lookUp(view, "NOSUCHCODE", EMAIL);
        assertNotFound(view);

        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");
        assertThat($(Paragraph.class, view).withId("error-message").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Lookup Locked
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Lookup Locked", businessRules = {"BR-003"})
    void fifth_failed_lookup_locks_the_lookup_even_for_a_correct_code() {
        Component view = openView();
        for (int attempt = 1; attempt <= 4; attempt++) {
            lookUp(view, "WRONG0000" + attempt, EMAIL);
            assertNotFound(view);
        }
        // The fifth failed attempt is still answered as "not found"
        lookUp(view, "WRONG00005", EMAIL);
        assertNotFound(view);

        // Now the lookup is refused without checking the reservation code
        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat(errorMessage(view)).isEqualTo(ViewOwnReservationView.LOCKED);
        assertThat($(H2.class, view).withText("Your reservation").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Lookup Locked", businessRules = {"BR-003"})
    void successful_lookup_does_not_reset_the_count() {
        Component view = openView();
        for (int attempt = 1; attempt <= 4; attempt++) {
            lookUp(view, "WRONG0000" + attempt, EMAIL);
        }
        lookUp(view, "LOOKUP0001", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");

        lookUp(view, "WRONG00005", EMAIL);
        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat(errorMessage(view)).isEqualTo(ViewOwnReservationView.LOCKED);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Lookup Locked", businessRules = {"BR-003"})
    void lookup_is_possible_again_after_15_minutes() {
        Component view = openView();
        for (int attempt = 1; attempt <= 5; attempt++) {
            lookUp(view, "WRONG0000" + attempt, EMAIL);
        }
        LOOKUP_LOCK.advance(Duration.ofMinutes(14));
        lookUp(view, "LOOKUP0001", EMAIL);
        assertThat(errorMessage(view)).isEqualTo(ViewOwnReservationView.LOCKED);

        LOOKUP_LOCK.advance(Duration.ofMinutes(1));
        lookUp(view, "LOOKUP0001", EMAIL);

        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0001");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Lookup Locked", businessRules = {"BR-003"})
    void lock_applies_to_the_network_address_only() {
        Component view = openView();
        for (int attempt = 1; attempt <= 5; attempt++) {
            lookUp(view, "WRONG0000" + attempt, EMAIL);
        }

        assertThat(LOOKUP_LOCK.isLocked("192.0.2.99")).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Reservation Can Be Cancelled Online
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A4: Reservation Can Be Cancelled Online")
    void confirmed_reservation_before_the_deadline_offers_the_cancellation() {
        Component view = openView();

        lookUp(view, "LOOKUP0001", EMAIL);
        assertThat(cancelOffered(view)).isTrue();

        // Arrival tomorrow: the deadline is today at 14:00, it is 12:00
        lookUp(view, "LOOKUP0002", EMAIL);
        assertThat(cancelOffered(view)).isTrue();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A4: Reservation Can Be Cancelled Online")
    void no_cancellation_offered_after_the_deadline_or_for_other_statuses() {
        Component view = openView();

        // Arrival today: the deadline was yesterday at 14:00
        lookUp(view, "LOOKUP0003", EMAIL);
        assertThat(paragraphTexts(view)).contains("Reservation code: LOOKUP0003");
        assertThat(cancelOffered(view)).isFalse();

        for (String code : List.of("LOOKUP0004", "LOOKUP0005", "LOOKUP0006")) {
            lookUp(view, code, EMAIL);
            assertThat(paragraphTexts(view)).contains("Reservation code: " + code);
            assertThat(cancelOffered(view)).as(code).isFalse();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Business rules
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Reservation Not Found", businessRules = {"BR-004"})
    void reservation_of_an_erased_guest_can_no_longer_be_found() {
        Component view = openView();

        lookUp(view, "ERASED0001", "dora.erased@example.com");
        assertNotFound(view);

        lookUp(view, "ERASED0001", "guest-1102@erased.invalid");
        assertNotFound(view);
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

    private boolean cancelOffered(Component view) {
        return $(Button.class, view).withText("Cancel reservation").exists();
    }

    private String errorMessage(Component view) {
        return $(Paragraph.class, view).withId("error-message").single().getText();
    }

    private void assertNotFound(Component view) {
        assertThat(errorMessage(view)).isEqualTo(ViewOwnReservationView.NOT_FOUND);
        assertThat($(H2.class, view).withText("Your reservation").exists()).isFalse();
    }

    private void assertNothingShown(Component view) {
        assertThat($(Paragraph.class, view).withId("error-message").exists()).isFalse();
        assertThat($(H2.class, view).withText("Your reservation").exists()).isFalse();
    }

    private List<String> paragraphTexts(Component view) {
        return $(Paragraph.class, view).all().stream().map(Paragraph::getText).toList();
    }
}
