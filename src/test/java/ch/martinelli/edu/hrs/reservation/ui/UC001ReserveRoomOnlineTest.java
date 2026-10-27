package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.GuestRecord;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reservation.OnlineReservationService;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASON_RATE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-001 "Reserve Room Online". Test data: V100__uc001_test_data.sql; today is 1 June 2030.
 */
class UC001ReserveRoomOnlineTest extends HrsBrowserlessTest {

    private static final LocalDate TODAY = LocalDate.of(2030, 6, 1);
    private static final long RIAD = 3301;
    private static final long AUTUMN_2030 = 3302;
    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();
    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    OnlineReservationService reservationService;

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
    void removeCreatedData() {
        EMAIL_SERVICE.reset();
        ctx.deleteFrom(RESERVATION).where(RESERVATION.ID.lt(FIRST_TEST_DATA_ID)).execute();
        ctx.deleteFrom(GUEST).where(GUEST.ID.lt(FIRST_TEST_DATA_ID)).execute();
        // A7 replaces the details of the known guest Ben Beispiel; restore them from the test data
        ctx.update(GUEST)
                .set(GUEST.FIRST_NAME, "Ben")
                .set(GUEST.LAST_NAME, "Beispiel")
                .set(GUEST.PHONE, "+41 79 222 22 22")
                .where(GUEST.ID.eq(1002L))
                .execute();
        // The BR-004 test changes the nightly rate of the Double Room; restore it from the test data
        ctx.update(ROOM_TYPE)
                .set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("150.00"))
                .where(ROOM_TYPE.ID.eq(1002L))
                .execute();
        // The season tests offer the Riad and change its season rate (V121); restore both
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, false).where(ROOM_TYPE.ID.eq(RIAD)).execute();
        ctx.update(SEASON_RATE).set(SEASON_RATE.NIGHTLY_RATE, new BigDecimal("160.00"))
                .where(SEASON_RATE.SEASON_ID.eq(AUTUMN_2030)).and(SEASON_RATE.ROOM_TYPE_ID.eq(RIAD))
                .execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002", "BR-003", "BR-004"})
    void search_shows_available_room_types_with_description_nightly_rate_and_total_price() {
        Component view = openView();

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 13), 2);

        // Step 3: Single Room (max. 1 guest) and the inactive Suite are not offered
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
        assertThat(paragraphTexts(view)).contains(
                "A room with a queen-size bed", "Nightly rate: CHF 150.00", "Total price for 3 nights: CHF 450.00",
                "A large room with two double beds", "Nightly rate: CHF 250.00",
                "Total price for 3 nights: CHF 750.00");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004", "BR-006", "BR-007", "BR-011"})
    void guest_reserves_room_type_and_receives_reservation_code_and_confirmation_email() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 13), 2);

        selectRoomType(view, "Double Room");

        // Step 5: summary of dates, room type, number of nights and total price
        assertThat(paragraphTexts(view)).contains(
                "Arrival date: Jun 10, 2030", "Departure date: Jun 13, 2030", "Room type: Double Room",
                "Number of nights: 3", "Total price: CHF 450.00");

        enterGuestDetails(view, "Clara", "Neu", "Clara.Neu@Example.com", "+41 79 333 33 33");
        confirm(view);

        // Step 11: confirmation with reservation code and total price
        String reservationCode = reservationCode(view);
        // UC-001 BR-007: at least 8 random characters
        assertThat(reservationCode).hasSizeGreaterThanOrEqualTo(8);
        assertThat($(Paragraph.class, view).withId("total-price").single().getText())
                .isEqualTo("Total price: CHF 450.00");
        assertThat($(Paragraph.class, view).withId("email-delayed").exists()).isFalse();

        // Step 9 / success postconditions: guest and reservation are recorded
        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.EMAIL.eq("clara.neu@example.com")).fetchSingle();
        assertThat(guest.getFirstName()).isEqualTo("Clara");
        assertThat(guest.getLastName()).isEqualTo("Neu");
        assertThat(guest.getPhone()).isEqualTo("+41 79 333 33 33");

        ReservationRecord reservation = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .fetchSingle();
        assertThat(reservation.getGuestId()).isEqualTo(guest.getId());
        assertThat(reservation.getRoomTypeId()).isEqualTo(1002L);
        assertThat(reservation.getArrivalDate()).isEqualTo(LocalDate.of(2030, 6, 10));
        assertThat(reservation.getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 13));
        assertThat(reservation.getNumberOfGuests()).isEqualTo(2);
        // UC-001 BR-004: 3 nights x CHF 150.00
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reservation.getChannel()).isEqualTo("ONLINE");
        assertThat(reservation.getRoomId()).isNull();
        assertThat(reservation.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
        // Step 9 / success postcondition: online cancellation deadline 48 hours before 14:00 on the arrival date
        assertThat(reservation.getOnlineCancellationDeadline()).isEqualTo(LocalDateTime.of(2030, 6, 8, 14, 0));

        // Step 10, UC-001 BR-011: confirmation email with code, dates, room type, number of guests, total price and
        // online cancellation deadline (UC-003 BR-006)
        List<Email> emails = EMAIL_SERVICE.sentEmails();
        assertThat(emails).hasSize(1);
        Email email = emails.getFirst();
        assertThat(email.to()).isEqualTo("clara.neu@example.com");
        assertThat(email.body()).contains(
                "Reservation code: " + reservationCode,
                "Arrival date: 2030-06-10",
                "Departure date: 2030-06-13",
                "Room type: Double Room",
                "Number of guests: 2",
                "Total price: CHF 450.00",
                "Free online cancellation before 8 June 2030, 14:00");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-011"})
    void confirmation_email_of_a_late_reservation_says_that_only_reception_can_cancel() {
        Component view = openView();
        // Arrival today: the online cancellation deadline (30 May, 14:00) has already passed
        search(view, TODAY, TODAY.plusDays(1), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(view);

        ReservationRecord reservation = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode(view)))
                .fetchSingle();
        assertThat(reservation.getOnlineCancellationDeadline()).isEqualTo(LocalDateTime.of(2030, 5, 30, 14, 0));
        // UC-003 BR-006: instead of the deadline the guest is told that reception can still cancel
        assertThat(EMAIL_SERVICE.sentEmails().getFirst().body())
                .contains("This reservation can no longer be cancelled online. "
                        + "The hotel's reception can still cancel it.")
                .doesNotContain("Free online cancellation");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-007"})
    void every_reservation_gets_its_own_reservation_code() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 20), LocalDate.of(2030, 6, 21), 1);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(view);
        String firstCode = reservationCode(view);

        Component secondView = openSecondView();
        search(secondView, LocalDate.of(2030, 6, 20), LocalDate.of(2030, 6, 21), 1);
        selectRoomType(secondView, "Double Room");
        enterGuestDetails(secondView, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(secondView);
        String secondCode = reservationCode(secondView);

        assertThat(secondCode).isNotEqualTo(firstCode);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // BR-002: Available room type
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void room_type_whose_rooms_are_all_reserved_is_not_offered_and_inactive_rooms_do_not_count() {
        Component view = openView();

        // Both active Double Rooms are reserved 1-3 October; the inactive room 203 does not make one available
        search(view, LocalDate.of(2030, 10, 1), LocalDate.of(2030, 10, 3), 2);

        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void cancelled_reservations_do_not_reduce_availability() {
        Component view = openView();

        // 1-3 September: one Confirmed and one Cancelled Double Room reservation for two active Double Rooms
        search(view, LocalDate.of(2030, 9, 1), LocalDate.of(2030, 9, 3), 2);

        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void room_type_reserved_for_one_night_of_the_range_is_not_offered() {
        Component view = openView();

        // The only Single Room is reserved for the night of 2 August
        search(view, LocalDate.of(2030, 7, 31), LocalDate.of(2030, 8, 2), 1);

        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void room_type_is_available_again_on_the_departure_date_of_a_reservation() {
        Component view = openView();

        // The Single Room reservation departs on 3 August; the night of 3 August is free
        search(view, LocalDate.of(2030, 8, 3), LocalDate.of(2030, 8, 4), 1);

        assertThat(offeredRoomTypes(view)).containsExactly("Single Room", "Double Room", "Family Room");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void room_type_is_available_for_the_nights_before_and_after_a_room_block() {
        Component view = openView();

        // The Family Room is blocked for the nights of 10 to 12 July
        search(view, LocalDate.of(2030, 7, 8), LocalDate.of(2030, 7, 10), 3);
        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");

        search(view, LocalDate.of(2030, 7, 13), LocalDate.of(2030, 7, 14), 3);
        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // BR-003: Number of guests
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-003"})
    void room_types_with_a_lower_maximum_number_of_guests_are_not_offered() {
        Component view = openView();

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 1);
        assertThat(offeredRoomTypes(view)).containsExactly("Single Room", "Double Room", "Family Room");

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 3);
        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid search input
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input", businessRules = {"BR-001"})
    void arrival_date_in_the_past_is_marked_invalid() {
        Component view = openView();

        search(view, TODAY.minusDays(1), TODAY.plusDays(1), 2);

        DatePicker arrivalDate = $(DatePicker.class, view).withCaption("Arrival date").single();
        assertThat(arrivalDate.isInvalid()).isTrue();
        assertThat(arrivalDate.getErrorMessage())
                .isEqualTo("The arrival date must be between today and Jun 1, 2031");
        assertNoSearchResults(view);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input", businessRules = {"BR-001"})
    void arrival_date_more_than_365_days_ahead_is_marked_invalid() {
        Component view = openView();

        search(view, TODAY.plusDays(366), TODAY.plusDays(367), 2);

        DatePicker arrivalDate = $(DatePicker.class, view).withCaption("Arrival date").single();
        assertThat(arrivalDate.isInvalid()).isTrue();
        assertThat(arrivalDate.getErrorMessage())
                .isEqualTo("The arrival date must be between today and Jun 1, 2031");
        assertNoSearchResults(view);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input", businessRules = {"BR-001"})
    void arrival_today_and_arrival_365_days_ahead_are_allowed() {
        Component view = openView();

        search(view, TODAY, TODAY.plusDays(1), 2);
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");

        // A long stay is allowed: BR-001 sets no maximum number of nights
        search(view, TODAY.plusDays(365), TODAY.plusDays(400), 2);
        assertThat($(DatePicker.class, view).withCaption("Arrival date").single().isInvalid()).isFalse();
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input", businessRules = {"BR-001"})
    void departure_date_not_after_arrival_date_is_marked_invalid() {
        Component view = openView();

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 10), 2);

        DatePicker departureDate = $(DatePicker.class, view).withCaption("Departure date").single();
        assertThat(departureDate.isInvalid()).isTrue();
        assertThat(departureDate.getErrorMessage()).isEqualTo("The departure date must be after the arrival date");
        assertNoSearchResults(view);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input", businessRules = {"BR-003"})
    void number_of_guests_outside_1_to_10_is_marked_invalid() {
        Component view = openView();
        IntegerField numberOfGuests = $(IntegerField.class, view).withCaption("Number of guests").single();

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 0);
        assertThat(numberOfGuests.isInvalid()).isTrue();
        assertThat(numberOfGuests.getErrorMessage()).isEqualTo("The number of guests must be between 1 and 10");
        assertNoSearchResults(view);

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 11);
        assertThat(numberOfGuests.isInvalid()).isTrue();
        assertThat(numberOfGuests.getErrorMessage()).isEqualTo("The number of guests must be between 1 and 10");
        assertNoSearchResults(view);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Invalid Search Input")
    void search_after_correcting_invalid_input_shows_room_types() {
        Component view = openView();
        search(view, TODAY.minusDays(1), TODAY.plusDays(1), 2);
        assertNoSearchResults(view);

        // A1 step 2: continues at step 2
        search(view, TODAY.plusDays(9), TODAY.plusDays(10), 2);

        assertThat($(DatePicker.class, view).withCaption("Arrival date").single().isInvalid()).isFalse();
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: No room type available
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A2: No Room Type Available", businessRules = {"BR-002", "BR-003"})
    void guest_is_informed_when_no_room_type_is_available() {
        Component view = openView();

        // Only the Family Room fits 3 guests, and it is blocked for the night of 10 July
        search(view, LocalDate.of(2030, 7, 9), LocalDate.of(2030, 7, 11), 3);

        assertThat(offeredRoomTypes(view)).isEmpty();
        assertThat($(Paragraph.class, view).withId("no-room-available").single().getText())
                .isEqualTo(ReserveRoomOnlineView.NO_ROOM_AVAILABLE);
        assertThat($(Button.class, view).withText("Confirm reservation").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Guest changes the search
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Guest Changes the Search", businessRules = {"BR-004"})
    void guest_changes_the_dates_and_sees_the_room_types_for_the_new_search() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 13), 2);
        assertThat(paragraphTexts(view)).contains("Total price for 3 nights: CHF 450.00");

        test($(DatePicker.class, view).withCaption("Departure date").single()).setValue(LocalDate.of(2030, 6, 11));
        // The room types of the old search are no longer shown
        assertThat(offeredRoomTypes(view)).isEmpty();

        test($(Button.class, view).withText("Search").single()).click();

        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
        assertThat(paragraphTexts(view)).contains(
                "Total price for 1 night: CHF 150.00", "Total price for 1 night: CHF 250.00");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Guest Changes the Search", businessRules = {"BR-003"})
    void guest_changes_the_number_of_guests_and_sees_the_room_types_for_the_new_search() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 1);
        assertThat(offeredRoomTypes(view)).containsExactly("Single Room", "Double Room", "Family Room");

        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 4);

        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Invalid guest details
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A4: Invalid Guest Details", businessRules = {"BR-008"})
    void missing_guest_details_are_marked_and_no_reservation_is_recorded() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(view, "Double Room");

        enterGuestDetails(view, "Clara", "", "", "");
        confirm(view);

        assertThat($(TextField.class, view).withCaption("First name").single().isInvalid()).isFalse();
        assertInvalid($(TextField.class, view).withCaption("Last name").single(), "Enter your last name");
        assertInvalid($(EmailField.class, view).withCaption("Email address").single(), "Enter your email address");
        assertInvalid($(TextField.class, view).withCaption("Phone number").single(), "Enter your phone number");
        // The entered value is kept
        assertThat($(TextField.class, view).withCaption("First name").single().getValue()).isEqualTo("Clara");
        assertNothingRecorded();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A4: Invalid Guest Details", businessRules = {"BR-008"})
    void invalid_email_address_is_marked_and_the_entered_values_are_kept() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(view, "Double Room");

        enterGuestDetails(view, "Clara", "Neu", "clara.neu-at-example.com", "+41 79 333 33 33");
        confirm(view);

        EmailField email = $(EmailField.class, view).withCaption("Email address").single();
        assertThat(email.isInvalid()).isTrue();
        assertThat(email.getValue()).isEqualTo("clara.neu-at-example.com");
        assertThat($(TextField.class, view).withCaption("First name").single().getValue()).isEqualTo("Clara");
        assertThat($(TextField.class, view).withCaption("Last name").single().getValue()).isEqualTo("Neu");
        assertThat($(TextField.class, view).withCaption("Phone number").single().getValue())
                .isEqualTo("+41 79 333 33 33");
        assertNothingRecorded();

        // A4 step 2: continues at step 6
        test(email).setValue("clara.neu@example.com");
        confirm(view);
        assertThat($(Paragraph.class, view).withId("reservation-code").exists()).isTrue();
    }

    @Test
    @Disabled("Known defect: an email address of only spaces is marked invalid by the EmailField's built-in check, "
            + "which shows no error message (NFR-008). Fix in ReserveRoomOnlineView, then enable this test.")
    @UseCase(id = "UC-001", scenario = "A4: Invalid Guest Details", businessRules = {"BR-008"})
    void guest_details_of_only_spaces_are_marked_as_missing_and_kept() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(view, "Double Room");

        enterGuestDetails(view, "   ", "   ", "   ", "   ");
        confirm(view);

        assertInvalid($(TextField.class, view).withCaption("First name").single(), "Enter your first name");
        assertInvalid($(TextField.class, view).withCaption("Last name").single(), "Enter your last name");
        assertInvalid($(EmailField.class, view).withCaption("Email address").single(), "Enter your email address");
        assertInvalid($(TextField.class, view).withCaption("Phone number").single(), "Enter your phone number");
        assertThat($(TextField.class, view).withCaption("First name").single().getValue()).isEqualTo("   ");
        assertThat($(Button.class, view).withText("Confirm reservation").exists()).isTrue();
        assertNothingRecorded();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void total_price_does_not_change_when_the_nightly_rate_changes_later() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 13), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(view);
        String reservationCode = reservationCode(view);

        // The manager raises the nightly rate of the Double Room afterwards (UC-016)
        ctx.update(ROOM_TYPE)
                .set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("200.00"))
                .where(ROOM_TYPE.ID.eq(1002L))
                .execute();

        // UC-001 BR-004: fixed at 3 nights x CHF 150.00 when the reservation was recorded
        assertThat(ctx.select(RESERVATION.TOTAL_PRICE)
                .from(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .fetchSingle(RESERVATION.TOTAL_PRICE))
                .isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void stay_crossing_a_season_boundary_is_priced_night_by_night() {
        // The season Autumn 2030 (5 to 7 November) sets 160.00 for the Riad (normal rate 100.00) and no season rate
        // for the Double Room (V121)
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.ACTIVE, true).where(ROOM_TYPE.ID.eq(RIAD)).execute();
        Component view = openView();
        search(view, LocalDate.of(2030, 11, 4), LocalDate.of(2030, 11, 9), 2);

        // Step 3: the lowest and the highest nightly rate when the nights have different nightly rates
        assertThat(offeredRoomTypes(view)).contains("Double Room", "Riad");
        assertThat(paragraphTexts(view)).contains(
                "A riad around a quiet courtyard", "Nightly rate: CHF 100.00 to CHF 160.00",
                "Total price for 5 nights: CHF 680.00",
                // UC-025 BR-003: a room type without a season rate keeps its normal rate
                "Nightly rate: CHF 150.00", "Total price for 5 nights: CHF 750.00");

        // Step 5: the night price of every night
        selectRoomType(view, "Riad");
        assertThat($(ListItem.class, $(UnorderedList.class, view).withId("night-prices").single()).all())
                .extracting(ListItem::getText)
                .containsExactly("Night of Nov 4, 2030: CHF 100.00", "Night of Nov 5, 2030: CHF 160.00",
                        "Night of Nov 6, 2030: CHF 160.00", "Night of Nov 7, 2030: CHF 160.00",
                        "Night of Nov 8, 2030: CHF 100.00");
        assertThat(paragraphTexts(view)).contains("Number of nights: 5", "Total price: CHF 680.00");

        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(view);
        String reservationCode = reservationCode(view);
        assertThat($(Paragraph.class, view).withId("total-price").single().getText())
                .isEqualTo("Total price: CHF 680.00");

        // Step 9: the night price of every night is recorded; the total price is their sum
        assertThat(nightPrices(reservationCode)).containsExactly(new BigDecimal("100.00"), new BigDecimal("160.00"),
                new BigDecimal("160.00"), new BigDecimal("160.00"), new BigDecimal("100.00"));
        assertThat(reservation(reservationCode).getTotalPrice()).isEqualByComparingTo("680.00");

        // BR-004: a season changed later does not change the recorded night prices
        ctx.update(SEASON_RATE).set(SEASON_RATE.NIGHTLY_RATE, new BigDecimal("200.00"))
                .where(SEASON_RATE.SEASON_ID.eq(AUTUMN_2030)).and(SEASON_RATE.ROOM_TYPE_ID.eq(RIAD))
                .execute();
        assertThat(nightPrices(reservationCode)).containsExactly(new BigDecimal("100.00"), new BigDecimal("160.00"),
                new BigDecimal("160.00"), new BigDecimal("160.00"), new BigDecimal("100.00"));
        assertThat(reservation(reservationCode).getTotalPrice()).isEqualByComparingTo("680.00");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void every_night_of_a_stay_without_season_has_the_normal_rate() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 13), 2);
        selectRoomType(view, "Double Room");

        assertThat($(ListItem.class, $(UnorderedList.class, view).withId("night-prices").single()).all())
                .extracting(ListItem::getText)
                .containsExactly("Night of Jun 10, 2030: CHF 150.00", "Night of Jun 11, 2030: CHF 150.00",
                        "Night of Jun 12, 2030: CHF 150.00");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");
        confirm(view);

        assertThat(nightPrices(reservationCode(view))).containsExactly(new BigDecimal("150.00"),
                new BigDecimal("150.00"), new BigDecimal("150.00"));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Room type no longer available
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A5: Room Type No Longer Available", businessRules = {"BR-002", "BR-005"})
    void guest_is_informed_when_another_guest_took_the_last_room_since_the_search() {
        LocalDate arrival = LocalDate.of(2030, 8, 10);
        LocalDate departure = LocalDate.of(2030, 8, 12);
        Component view = openView();
        search(view, arrival, departure, 1);
        selectRoomType(view, "Single Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");

        // Another guest reserves the only Single Room for the same nights in their own online reservation
        Component otherGuestView = openSecondView();
        search(otherGuestView, arrival, departure, 1);
        selectRoomType(otherGuestView, "Single Room");
        enterGuestDetails(otherGuestView, "Dora", "Andere", "dora.andere@example.com", "+41 79 444 44 44");
        confirm(otherGuestView);
        assertThat($(Paragraph.class, otherGuestView).withId("reservation-code").exists()).isTrue();

        confirm(view);

        // A5 step 1: the guest is informed
        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(ReserveRoomOnlineView.ROOM_TYPE_NO_LONGER_AVAILABLE);
        // A5 step 2: continues at step 3 with the room types still available
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");

        // Failure postconditions: no overbooking, no second reservation, no guest recorded for the refused one
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_TYPE_ID.eq(1001L)
                .and(RESERVATION.ARRIVAL_DATE.eq(arrival)))).isEqualTo(1);
        assertThat(ctx.fetchExists(GUEST, GUEST.EMAIL.eq("clara.neu@example.com"))).isFalse();
        assertThat(EMAIL_SERVICE.sentEmails()).extracting(Email::to).containsExactly("dora.andere@example.com");

        // The guest continues with another room type; the entered guest details are kept (A5 step 1)
        selectRoomType(view, "Double Room");
        assertThat($(TextField.class, view).withCaption("First name").single().getValue()).isEqualTo("Clara");
        assertThat($(TextField.class, view).withCaption("Last name").single().getValue()).isEqualTo("Neu");
        assertThat($(EmailField.class, view).withCaption("Email address").single().getValue())
                .isEqualTo("clara.neu@example.com");
        assertThat($(TextField.class, view).withCaption("Phone number").single().getValue())
                .isEqualTo("+41 79 333 33 33");
        confirm(view);
        assertThat($(Paragraph.class, view).withId("reservation-code").exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A5: Room Type No Longer Available", businessRules = {"BR-005", "BR-006"})
    void stored_data_of_a_known_guest_is_not_changed_when_the_reservation_fails() {
        LocalDate arrival = LocalDate.of(2030, 8, 15);
        LocalDate departure = LocalDate.of(2030, 8, 16);
        Component view = openView();
        search(view, arrival, departure, 1);
        selectRoomType(view, "Single Room");
        // The known guest Ben Beispiel enters a new name and phone number
        enterGuestDetails(view, "Benjamin", "Beispiel-Muster", "ben.beispiel@example.com", "+41 79 999 99 99");

        Component otherGuestView = openSecondView();
        search(otherGuestView, arrival, departure, 1);
        selectRoomType(otherGuestView, "Single Room");
        enterGuestDetails(otherGuestView, "Dora", "Andere", "dora.andere@example.com", "+41 79 444 44 44");
        confirm(otherGuestView);

        confirm(view);

        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(ReserveRoomOnlineView.ROOM_TYPE_NO_LONGER_AVAILABLE);
        // Failure postcondition: the stored data of the existing guest is not changed
        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.ID.eq(1002L)).fetchSingle();
        assertThat(guest.getFirstName()).isEqualTo("Ben");
        assertThat(guest.getLastName()).isEqualTo("Beispiel");
        assertThat(guest.getPhone()).isEqualTo("+41 79 222 22 22");
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.GUEST_ID.eq(1002L))).isZero();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A6: Guest abandons the reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A6: Guest Abandons the Reservation")
    void entered_dates_and_guest_details_are_discarded_when_the_guest_leaves() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");

        // The guest leaves without confirming: their browser page and its Vaadin session end
        cleanVaadinEnvironment();
        initVaadinEnvironment();

        // A6 step 1: nothing entered is kept and nothing is recorded
        Component reopenedView = openView();
        assertThat($(DatePicker.class, reopenedView).withCaption("Arrival date").single().getValue()).isNull();
        assertThat($(DatePicker.class, reopenedView).withCaption("Departure date").single().getValue()).isNull();
        assertThat(offeredRoomTypes(reopenedView)).isEmpty();
        assertThat($(TextField.class, reopenedView).withCaption("First name").exists()).isFalse();
        assertNothingRecorded();
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();

        // Searching again shows empty guest details
        search(reopenedView, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(reopenedView, "Double Room");
        assertThat($(TextField.class, reopenedView).withCaption("First name").single().getValue()).isEmpty();
        assertThat($(TextField.class, reopenedView).withCaption("Last name").single().getValue()).isEmpty();
        assertThat($(EmailField.class, reopenedView).withCaption("Email address").single().getValue()).isEmpty();
        assertThat($(TextField.class, reopenedView).withCaption("Phone number").single().getValue()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A7: Guest already known
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A7: Guest Already Known", businessRules = {"BR-006"})
    void reservation_of_a_known_guest_is_linked_to_them_and_their_details_are_replaced() {
        int guestsBefore = ctx.fetchCount(GUEST);
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 12), 2);
        selectRoomType(view, "Double Room");

        // Same email address as the stored guest Ben Beispiel, in different case
        enterGuestDetails(view, "Benjamin", "Beispiel-Muster", "Ben.Beispiel@EXAMPLE.com", "+41 79 999 99 99");
        confirm(view);

        String reservationCode = reservationCode(view);
        ReservationRecord reservation = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .fetchSingle();
        assertThat(reservation.getGuestId()).isEqualTo(1002L);
        assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reservation.getChannel()).isEqualTo("ONLINE");

        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.ID.eq(1002L)).fetchSingle();
        assertThat(guest.getFirstName()).isEqualTo("Benjamin");
        assertThat(guest.getLastName()).isEqualTo("Beispiel-Muster");
        assertThat(guest.getPhone()).isEqualTo("+41 79 999 99 99");
        assertThat(guest.getEmail()).isEqualTo("ben.beispiel@example.com");
        assertThat(ctx.fetchCount(GUEST)).isEqualTo(guestsBefore);

        // A7 step 3: continues at step 10
        assertThat(EMAIL_SERVICE.sentEmails()).extracting(Email::to).containsExactly("ben.beispiel@example.com");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A8: Email Service does not accept the confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A8: Email Service Does Not Accept the Confirmation",
            businessRules = {"BR-012"})
    void reservation_stays_confirmed_and_guest_is_advised_to_note_the_code_when_the_email_is_refused() {
        EMAIL_SERVICE.setRefusing(true);
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Clara", "Neu", "clara.neu@example.com", "+41 79 333 33 33");

        confirm(view);

        // A8 steps 2 and 3: advice and confirmation with reservation code and total price
        assertThat($(Paragraph.class, view).withId("email-delayed").single().getText())
                .isEqualTo(ReserveRoomOnlineView.EMAIL_MAY_BE_DELAYED);
        String reservationCode = reservationCode(view);
        assertThat($(Paragraph.class, view).withId("total-price").single().getText())
                .isEqualTo("Total price: CHF 150.00");

        // A8 step 1 / BR-012: the reservation is kept unchanged and stays Confirmed
        ReservationRecord reservation = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .fetchSingle();
        assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // BR-008, BR-009, BR-010: Guest details, privacy notice, no online payment
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-008", "BR-009", "BR-010"})
    void privacy_notice_and_only_name_email_and_phone_are_shown_before_the_guest_can_confirm() {
        Component view = openView();
        search(view, LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 11), 2);
        assertThat($(Button.class, view).withText("Confirm reservation").exists()).isFalse();

        selectRoomType(view, "Double Room");

        // UC-001 BR-009: the privacy notice is displayed together with the confirm button
        assertThat($(Paragraph.class, view).withId("privacy-notice").single().getText())
                .isEqualTo(ReserveRoomOnlineView.PRIVACY_NOTICE);
        assertThat($(Button.class, view).withText("Confirm reservation").exists()).isTrue();

        // UC-001 BR-008, BR-010: first name, last name, email address and phone number; no payment data
        assertThat($(TextField.class, view).all()).extracting(TextField::getLabel)
                .containsExactlyInAnyOrder("First name", "Last name", "Phone number");
        assertThat($(EmailField.class, view).all()).extracting(EmailField::getLabel)
                .containsExactly("Email address");
        assertThat($(TextField.class, view).all()).allMatch(TextField::isRequiredIndicatorVisible);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private Component openView() {
        navigate(ReserveRoomOnlineView.class);
        return (Component) getCurrentView();
    }

    /**
     * A second online reservation in the same UI, standing in for another guest's browser.
     */
    private Component openSecondView() {
        ReserveRoomOnlineView secondView = new ReserveRoomOnlineView(reservationService, clock);
        UI.getCurrent().add(secondView);
        return secondView;
    }

    private void search(Component view, LocalDate arrival, LocalDate departure, Integer numberOfGuests) {
        test($(DatePicker.class, view).withCaption("Arrival date").single()).setValue(arrival);
        test($(DatePicker.class, view).withCaption("Departure date").single()).setValue(departure);
        test($(IntegerField.class, view).withCaption("Number of guests").single()).setValue(numberOfGuests);
        test($(Button.class, view).withText("Search").single()).click();
    }

    private void selectRoomType(Component view, String roomTypeName) {
        test($(Button.class, view).withAttribute("aria-label", "Select " + roomTypeName).single()).click();
    }

    private void enterGuestDetails(Component view, String firstName, String lastName, String email, String phone) {
        test($(TextField.class, view).withCaption("First name").single()).setValue(firstName);
        test($(TextField.class, view).withCaption("Last name").single()).setValue(lastName);
        test($(EmailField.class, view).withCaption("Email address").single()).setValue(email);
        test($(TextField.class, view).withCaption("Phone number").single()).setValue(phone);
    }

    private void confirm(Component view) {
        test($(Button.class, view).withText("Confirm reservation").single()).click();
    }

    private String reservationCode(Component view) {
        String text = $(Paragraph.class, view).withId("reservation-code").single().getText();
        assertThat(text).startsWith("Reservation code: ");
        return text.substring("Reservation code: ".length());
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }

    private List<BigDecimal> nightPrices(String reservationCode) {
        return ctx.select(RESERVATION_NIGHT.NIGHT_PRICE).from(RESERVATION_NIGHT)
                .join(RESERVATION).on(RESERVATION.ID.eq(RESERVATION_NIGHT.RESERVATION_ID))
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .orderBy(RESERVATION_NIGHT.NIGHT_DATE)
                .fetch(RESERVATION_NIGHT.NIGHT_PRICE);
    }

    private List<String> offeredRoomTypes(Component view) {
        return $(H3.class, view).all().stream().map(H3::getText).toList();
    }

    private List<String> paragraphTexts(Component view) {
        return $(Paragraph.class, view).all().stream().map(Paragraph::getText).toList();
    }

    private void assertNoSearchResults(Component view) {
        assertThat(offeredRoomTypes(view)).isEmpty();
        assertThat($(H2.class, view).withText("Available room types").exists()).isFalse();
        assertThat($(Paragraph.class, view).withId("no-room-available").exists()).isFalse();
    }

    private static void assertInvalid(HasValidation field, String errorMessage) {
        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo(errorMessage);
    }

    private void assertNothingRecorded() {
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ID.lt(FIRST_TEST_DATA_ID))).isZero();
        assertThat(ctx.fetchCount(GUEST, GUEST.ID.lt(FIRST_TEST_DATA_ID))).isZero();
    }
}
