package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.GuestRecord;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.Email;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-004 "Create Reservation". Test data: V100__uc001_test_data.sql (room types and rooms),
 * V103__uc022_test_data.sql (staff members, password Secret-123) and V104__uc004_test_data.sql; today is
 * 1 June 2030.
 */
@WithAnonymousUser
class UC004CreateReservationTest extends HrsBrowserlessTest {

    private static final LocalDate TODAY = LocalDate.of(2030, 6, 1);
    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();
    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;
    private static final long KNOWN_GUEST_ID = 1401;

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    ReceptionReservationService reservationService;

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
        SecurityContextHolder.clearContext();
        ctx.deleteFrom(RESERVATION).where(RESERVATION.ID.lt(FIRST_TEST_DATA_ID)).execute();
        ctx.deleteFrom(GUEST).where(GUEST.ID.lt(FIRST_TEST_DATA_ID)).execute();
        // BR-005 replaces the details of the known guest Felix Bekannt; restore them from the test data
        ctx.update(GUEST)
                .set(GUEST.FIRST_NAME, "Felix")
                .set(GUEST.LAST_NAME, "Bekannt")
                .set(GUEST.PHONE, "+41 79 666 66 66")
                .where(GUEST.ID.eq(KNOWN_GUEST_ID))
                .execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-002", "BR-004", "BR-006", "BR-007"})
    void receptionist_creates_reservation_for_a_new_guest() {
        logIn("rita");

        // Step 1: the receptionist opens a new reservation
        test($(RouterLink.class).withText("New reservation").single()).click();
        assertThat(getCurrentView()).isInstanceOf(CreateReservationView.class);
        Component view = (Component) getCurrentView();

        // Step 2
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 13), 2);

        // Step 3: Single Room (max. 1 guest) and the inactive Suite are not offered
        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
        assertThat(paragraphTexts(view)).contains(
                "Nightly rate: CHF 150.00", "Total price for 3 nights: CHF 450.00", "Available rooms: 2",
                "Nightly rate: CHF 250.00", "Total price for 3 nights: CHF 750.00", "Available rooms: 1");

        // Step 4
        selectRoomType(view, "Double Room");

        // Step 5 and A3: no guest has this email address, the other fields stay empty
        enterEmail(view, "Greta.Neu@Example.com");
        assertThat(firstName(view).getValue()).isEmpty();
        assertThat(lastName(view).getValue()).isEmpty();
        assertThat(phone(view).getValue()).isEmpty();

        // Step 7
        enterNameAndPhone(view, "Greta", "Neu", "+41 79 777 77 77");
        // Step 8
        confirm(view);

        // Step 12: reservation code and total price
        String reservationCode = reservationCode(view);
        assertThat(reservationCode).isNotBlank();
        assertThat($(Paragraph.class, view).withId("total-price").single().getText())
                .isEqualTo("Total price: CHF 450.00");
        assertThat($(Paragraph.class, view).withId("email-not-sent").exists()).isFalse();

        // Step 10 / success postconditions
        ReservationRecord reservation = reservation(reservationCode);
        assertThat(reservation.getStatus()).isEqualTo("CONFIRMED");
        assertThat(reservation.getChannel()).isEqualTo("RECEPTION");
        assertThat(reservation.getRoomTypeId()).isEqualTo(1002L);
        assertThat(reservation.getArrivalDate()).isEqualTo(LocalDate.of(2031, 1, 10));
        assertThat(reservation.getDepartureDate()).isEqualTo(LocalDate.of(2031, 1, 13));
        assertThat(reservation.getNumberOfGuests()).isEqualTo(2);
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(reservation.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));

        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.ID.eq(reservation.getGuestId())).fetchSingle();
        assertThat(guest.getFirstName()).isEqualTo("Greta");
        assertThat(guest.getLastName()).isEqualTo("Neu");
        // BR-005: the email address is compared case-insensitively
        assertThat(guest.getEmail()).isEqualTo("greta.neu@example.com");
        assertThat(guest.getPhone()).isEqualTo("+41 79 777 77 77");

        // Step 11: confirmation email with code, dates, room type, number of guests and total price
        assertThat(EMAIL_SERVICE.sentEmails()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("greta.neu@example.com");
            assertThat(email.body()).contains("Reservation code: " + reservationCode, "Arrival date: 2031-01-10",
                    "Departure date: 2031-01-13", "Room type: Double Room", "Number of guests: 2",
                    "Total price: CHF 450.00");
        });
    }

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-002"})
    void available_rooms_are_reduced_by_existing_reservations() {
        Component view = openView();

        // One of the two Double Rooms is reserved for the nights of 10 and 11 February 2031
        search(view, LocalDate.of(2031, 2, 11), LocalDate.of(2031, 2, 13), 2);

        assertThat(offeredRoomTypes(view)).containsExactly("Double Room", "Family Room");
        assertThat(availableRooms(view, "Double Room")).isEqualTo("Available rooms: 1");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-002"})
    void availability_of_the_room_type_is_reduced_by_the_new_reservation() {
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 20), LocalDate.of(2031, 1, 21), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Greta", "Neu", "greta.neu@example.com", "+41 79 777 77 77");
        confirm(view);
        assertThat(reservationCode(view)).isNotBlank();

        // Success postcondition: the availability of the room type is reduced by one for every night
        Component nextView = openView();
        search(nextView, LocalDate.of(2031, 1, 20), LocalDate.of(2031, 1, 21), 2);
        assertThat(availableRooms(nextView, "Double Room")).isEqualTo("Available rooms: 1");
    }

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-001"})
    void walk_in_guest_is_reserved_for_today() {
        Component view = openView();

        // The arrival date is preset with today
        assertThat($(DatePicker.class, view).withCaption("Arrival date").single().getValue()).isEqualTo(TODAY);
        // The only Single Room is taken tonight (V101), a Double Room is still free
        search(view, TODAY, TODAY.plusDays(1), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Hugo", "Spontan", "hugo.spontan@example.com", "+41 79 888 88 88");
        confirm(view);

        ReservationRecord reservation = reservation(reservationCode(view));
        assertThat(reservation.getArrivalDate()).isEqualTo(TODAY);
        assertThat(reservation.getChannel()).isEqualTo("RECEPTION");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Step 6 / BR-005: Guest identified by email address
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-005"})
    void stored_details_of_a_known_guest_are_displayed_and_replaced() {
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 12), 2);
        selectRoomType(view, "Double Room");

        // Step 6: the stored details are displayed; the email address is compared case-insensitively
        enterEmail(view, "Felix.Bekannt@EXAMPLE.com");
        assertThat(firstName(view).getValue()).isEqualTo("Felix");
        assertThat(lastName(view).getValue()).isEqualTo("Bekannt");
        assertThat(phone(view).getValue()).isEqualTo("+41 79 666 66 66");

        // Step 7: the receptionist corrects the phone number
        test(phone(view)).setValue("+41 79 999 99 99");
        confirm(view);

        ReservationRecord reservation = reservation(reservationCode(view));
        assertThat(reservation.getGuestId()).isEqualTo(KNOWN_GUEST_ID);
        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.ID.eq(KNOWN_GUEST_ID)).fetchSingle();
        assertThat(guest.getFirstName()).isEqualTo("Felix");
        assertThat(guest.getLastName()).isEqualTo("Bekannt");
        assertThat(guest.getPhone()).isEqualTo("+41 79 999 99 99");
        assertThat(ctx.fetchCount(GUEST, GUEST.EMAIL.eq("felix.bekannt@example.com"))).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid dates or number of guests
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A1: Invalid Dates or Number of Guests", businessRules = {"BR-001"})
    void arrival_date_in_the_past_is_marked_invalid() {
        Component view = openView();

        search(view, TODAY.minusDays(1), TODAY.plusDays(1), 2);

        DatePicker arrival = $(DatePicker.class, view).withCaption("Arrival date").single();
        assertThat(arrival.isInvalid()).isTrue();
        assertThat(arrival.getErrorMessage()).isEqualTo("The arrival date must be between today and Jun 1, 2031");
        assertThat(offeredRoomTypes(view)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A1: Invalid Dates or Number of Guests", businessRules = {"BR-001"})
    void arrival_date_more_than_365_days_ahead_is_marked_invalid() {
        Component view = openView();

        search(view, TODAY.plusDays(366), TODAY.plusDays(367), 2);

        assertThat($(DatePicker.class, view).withCaption("Arrival date").single().isInvalid()).isTrue();
        assertThat(offeredRoomTypes(view)).isEmpty();

        // 365 days ahead is still allowed
        search(view, TODAY.plusDays(365), TODAY.plusDays(366), 2);
        assertThat($(DatePicker.class, view).withCaption("Arrival date").single().isInvalid()).isFalse();
        assertThat(offeredRoomTypes(view)).isNotEmpty();
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A1: Invalid Dates or Number of Guests", businessRules = {"BR-001"})
    void departure_date_not_after_arrival_date_is_marked_invalid() {
        Component view = openView();

        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 10), 2);

        DatePicker departure = $(DatePicker.class, view).withCaption("Departure date").single();
        assertThat(departure.isInvalid()).isTrue();
        assertThat(departure.getErrorMessage()).isEqualTo("The departure date must be after the arrival date");
        assertThat(offeredRoomTypes(view)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A1: Invalid Dates or Number of Guests", businessRules = {"BR-002"})
    void number_of_guests_outside_1_to_10_is_marked_invalid() {
        Component view = openView();
        IntegerField guests = $(IntegerField.class, view).withCaption("Number of guests").single();

        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 0);
        assertThat(guests.isInvalid()).isTrue();
        assertThat(guests.getErrorMessage()).isEqualTo("The number of guests must be between 1 and 10");
        assertThat(offeredRoomTypes(view)).isEmpty();

        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 11);
        assertThat(guests.isInvalid()).isTrue();
        assertThat(offeredRoomTypes(view)).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: No room type available
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A2: No Room Type Available", businessRules = {"BR-002"})
    void receptionist_is_informed_when_no_room_type_fits_the_number_of_guests() {
        Component view = openView();

        // No active room type takes more than 4 guests
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 5);

        assertThat($(Paragraph.class, view).withId("no-room-type-available").single().getText())
                .isEqualTo("No room type is available for these dates and number of guests.");
        assertThat(offeredRoomTypes(view)).isEmpty();

        // A2 step 2: continues at step 2
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 4);
        assertThat(offeredRoomTypes(view)).containsExactly("Family Room");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Invalid guest details
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A4: Invalid Guest Details", businessRules = {"BR-006"})
    void missing_guest_details_are_marked_and_nothing_is_recorded() {
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 2);
        selectRoomType(view, "Double Room");
        enterEmail(view, "greta.neu@example.com");
        test(firstName(view)).setValue("Greta");

        confirm(view);

        assertThat(firstName(view).isInvalid()).isFalse();
        assertThat(lastName(view).isInvalid()).isTrue();
        assertThat(phone(view).isInvalid()).isTrue();
        // The entered values are kept
        assertThat(firstName(view).getValue()).isEqualTo("Greta");
        assertThat(email(view).getValue()).isEqualTo("greta.neu@example.com");
        assertNothingRecorded();
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A4: Invalid Guest Details", businessRules = {"BR-006"})
    void invalid_email_address_is_marked_and_nothing_is_recorded() {
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Greta", "Neu", "greta.neu-at-example.com", "+41 79 777 77 77");

        confirm(view);

        assertThat(email(view).isInvalid()).isTrue();
        assertThat(email(view).getValue()).isEqualTo("greta.neu-at-example.com");
        assertThat(lastName(view).getValue()).isEqualTo("Neu");
        assertNothingRecorded();

        // A4 step 2: continues at step 5
        enterEmail(view, "greta.neu@example.com");
        confirm(view);
        assertThat(reservationCode(view)).isNotBlank();
    }

    @Test
    @UseCase(id = "UC-004", businessRules = {"BR-006"})
    void only_name_email_and_phone_are_asked_for() {
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 2);
        selectRoomType(view, "Double Room");

        assertThat($(TextField.class, view).all()).extracting(TextField::getLabel)
                .containsExactlyInAnyOrder("First name", "Last name", "Phone number");
        assertThat($(EmailField.class, view).all()).extracting(EmailField::getLabel)
                .containsExactly("Email address");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Room type no longer available
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A5: Room Type No Longer Available", businessRules = {"BR-002", "BR-003"})
    void receptionist_is_informed_when_another_reservation_took_the_last_room() {
        LocalDate arrival = LocalDate.of(2031, 3, 10);
        LocalDate departure = LocalDate.of(2031, 3, 12);
        Component view = openView();
        search(view, arrival, departure, 3);
        selectRoomType(view, "Family Room");
        enterGuestDetails(view, "Greta", "Neu", "greta.neu@example.com", "+41 79 777 77 77");

        // Another receptionist reserves the only Family Room for the same nights
        Component otherView = openSecondView();
        search(otherView, arrival, departure, 3);
        selectRoomType(otherView, "Family Room");
        enterGuestDetails(otherView, "Ida", "Andere", "ida.andere@example.com", "+41 79 123 45 67");
        confirm(otherView);
        assertThat($(Paragraph.class, otherView).withId("reservation-code").exists()).isTrue();

        confirm(view);

        // A5 step 1: the receptionist is informed
        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(CreateReservationView.ROOM_TYPE_NO_LONGER_AVAILABLE);
        // A5 step 2: continues at step 3; no room type takes 3 guests any more
        assertThat(offeredRoomTypes(view)).isEmpty();

        // Failure postconditions: no overbooking, no reservation and no guest recorded for the refused one
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_TYPE_ID.eq(1003L)
                .and(RESERVATION.ARRIVAL_DATE.eq(arrival)))).isEqualTo(1);
        assertThat(ctx.fetchExists(GUEST, GUEST.EMAIL.eq("greta.neu@example.com"))).isFalse();
        assertThat(EMAIL_SERVICE.sentEmails()).extracting(Email::to).containsExactly("ida.andere@example.com");

        // The entered guest details are kept
        search(view, arrival, departure, 2);
        selectRoomType(view, "Double Room");
        assertThat(firstName(view).getValue()).isEqualTo("Greta");
        assertThat(lastName(view).getValue()).isEqualTo("Neu");
        assertThat(email(view).getValue()).isEqualTo("greta.neu@example.com");
        assertThat(phone(view).getValue()).isEqualTo("+41 79 777 77 77");
    }

    @Test
    @UseCase(id = "UC-004", scenario = "A5: Room Type No Longer Available", businessRules = {"BR-003", "BR-005"})
    void stored_data_of_a_known_guest_is_not_changed_when_the_reservation_fails() {
        LocalDate arrival = LocalDate.of(2031, 3, 15);
        LocalDate departure = LocalDate.of(2031, 3, 16);
        Component view = openView();
        search(view, arrival, departure, 1);
        selectRoomType(view, "Single Room");
        enterEmail(view, "felix.bekannt@example.com");
        test(phone(view)).setValue("+41 79 999 99 99");

        Component otherView = openSecondView();
        search(otherView, arrival, departure, 1);
        selectRoomType(otherView, "Single Room");
        enterGuestDetails(otherView, "Ida", "Andere", "ida.andere@example.com", "+41 79 123 45 67");
        confirm(otherView);

        confirm(view);

        assertThat($(Paragraph.class, view).withId("error-message").single().getText())
                .isEqualTo(CreateReservationView.ROOM_TYPE_NO_LONGER_AVAILABLE);
        GuestRecord guest = ctx.selectFrom(GUEST).where(GUEST.ID.eq(KNOWN_GUEST_ID)).fetchSingle();
        assertThat(guest.getPhone()).isEqualTo("+41 79 666 66 66");
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.GUEST_ID.eq(KNOWN_GUEST_ID))).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A6: Receptionist discards the reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A6: Receptionist Discards the Reservation")
    void entered_data_is_discarded_when_the_receptionist_leaves() {
        logIn("rita");
        Component view = navigate(CreateReservationView.class);
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Greta", "Neu", "greta.neu@example.com", "+41 79 777 77 77");

        test($(Button.class, view).withText("Discard").single()).click();

        // A6 step 2: the use case ends
        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);
        // A6 step 1: nothing is recorded and the entered data is gone
        assertNothingRecorded();
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
        Component reopened = navigate(CreateReservationView.class);
        assertThat($(DatePicker.class, reopened).withCaption("Departure date").single().getValue()).isNull();
        assertThat(offeredRoomTypes(reopened)).isEmpty();
        assertThat($(TextField.class, reopened).withCaption("First name").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A7: Email Service does not accept the confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004", scenario = "A7: Email Service Does Not Accept the Confirmation",
            businessRules = {"BR-007"})
    void receptionist_is_advised_to_tell_the_reservation_code_when_the_email_is_refused() {
        EMAIL_SERVICE.setRefusing(true);
        Component view = openView();
        search(view, LocalDate.of(2031, 1, 10), LocalDate.of(2031, 1, 11), 2);
        selectRoomType(view, "Double Room");
        enterGuestDetails(view, "Greta", "Neu", "greta.neu@example.com", "+41 79 777 77 77");

        confirm(view);

        // A7 step 3: continues at step 12
        String reservationCode = reservationCode(view);
        // A7 step 2
        assertThat($(Paragraph.class, view).withId("email-not-sent").single().getText())
                .isEqualTo(CreateReservationView.TELL_GUEST_RESERVATION_CODE);
        // A7 step 1: the reservation stays unchanged
        assertThat(reservation(reservationCode).getStatus()).isEqualTo("CONFIRMED");
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-004")
    void only_a_logged_in_receptionist_can_create_a_reservation() {
        // Without login the staff login is displayed
        navigate("new-reservation", LoginView.class);

        logIn("hanna");
        assertThat($(RouterLink.class).withText("New reservation").exists()).isFalse();
        assertThat(navigate("new-reservation", RouteNotFoundError.class)).isNotNull();
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

    private Component openView() {
        logIn("rita");
        return navigate(CreateReservationView.class);
    }

    /**
     * A second new reservation in the same UI, standing in for another receptionist's browser.
     */
    private Component openSecondView() {
        CreateReservationView secondView = new CreateReservationView(reservationService, clock);
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

    private void enterEmail(Component view, String emailAddress) {
        test(email(view)).setValue(emailAddress);
    }

    private void enterNameAndPhone(Component view, String first, String last, String phoneNumber) {
        test(firstName(view)).setValue(first);
        test(lastName(view)).setValue(last);
        test(phone(view)).setValue(phoneNumber);
    }

    private void enterGuestDetails(Component view, String first, String last, String emailAddress,
                                   String phoneNumber) {
        enterEmail(view, emailAddress);
        enterNameAndPhone(view, first, last, phoneNumber);
    }

    private void confirm(Component view) {
        test($(Button.class, view).withText("Confirm reservation").single()).click();
    }

    private EmailField email(Component view) {
        return $(EmailField.class, view).withCaption("Email address").single();
    }

    private TextField firstName(Component view) {
        return $(TextField.class, view).withCaption("First name").single();
    }

    private TextField lastName(Component view) {
        return $(TextField.class, view).withCaption("Last name").single();
    }

    private TextField phone(Component view) {
        return $(TextField.class, view).withCaption("Phone number").single();
    }

    private String reservationCode(Component view) {
        return $(Paragraph.class, view).withId("reservation-code").single().getText()
                .substring("Reservation code: ".length());
    }

    private List<String> offeredRoomTypes(Component view) {
        return $(H3.class, view).all().stream().map(H3::getText).toList();
    }

    private List<String> paragraphTexts(Component view) {
        return $(Paragraph.class, view).all().stream().map(Paragraph::getText).toList();
    }

    private String availableRooms(Component view, String roomTypeName) {
        H3 heading = $(H3.class, view).withText(roomTypeName).single();
        Component card = heading.getParent().orElseThrow();
        return $(Paragraph.class, card).withTextContaining("Available rooms").single().getText();
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }

    private void assertNothingRecorded() {
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ID.lt(FIRST_TEST_DATA_ID))).isZero();
        assertThat(ctx.fetchCount(GUEST, GUEST.ID.lt(FIRST_TEST_DATA_ID))).isZero();
    }
}
