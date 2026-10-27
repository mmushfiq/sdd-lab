package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationNightRecord;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.reservation.InvalidChangeException;
import ch.martinelli.edu.hrs.reservation.ReceptionReservationService;
import ch.martinelli.edu.hrs.reservation.ReceptionRoomType;
import ch.martinelli.edu.hrs.reservation.ReservationChange;
import ch.martinelli.edu.hrs.reservation.ReservationChangeService;
import ch.martinelli.edu.hrs.reservation.ReservationNotChangeableException;
import ch.martinelli.edu.hrs.reservation.RoomTypeChoice;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-005 "Change Reservation". Test data: V106__uc005_test_data.sql, room types from
 * V100__uc001_test_data.sql (Double Room 150.00, Family Room 250.00 with one room, inactive Suite) and staff members
 * from V103__uc022_test_data.sql (password Secret-123); today is 1 June 2030.
 */
@WithAnonymousUser
class UC005ChangeReservationTest extends HrsBrowserlessTest {

    private static final RecordingEmailService EMAIL_SERVICE = new RecordingEmailService();

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    ReservationChangeService changeService;

    @Autowired
    ReceptionReservationService receptionReservationService;

    @Autowired
    DSLContext ctx;

    private Result<ReservationRecord> originalReservations;
    private Result<ReservationNightRecord> originalNights;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    static EmailService emailService() {
        return EMAIL_SERVICE;
    }

    @BeforeEach
    void rememberTestData() {
        EMAIL_SERVICE.reset();
        originalReservations = ctx.selectFrom(RESERVATION).where(RESERVATION.ID.between(1801L, 1899L)
                .or(RESERVATION.ID.between(3301L, 3399L))).fetch();
        originalNights = ctx.selectFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.between(1801L, 1899L)
                .or(RESERVATION_NIGHT.RESERVATION_ID.between(3301L, 3399L))).fetch();
    }

    @AfterEach
    void restoreTestData() {
        EMAIL_SERVICE.reset();
        SecurityContextHolder.clearContext();
        // Changes recorded by the tests are undone with the values from the test data
        originalReservations.forEach(original -> {
            original.changed(true);
            original.changed(RESERVATION.ID, false);
            ctx.update(RESERVATION).set(original).where(RESERVATION.ID.eq(original.getId())).execute();
        });
        // The night prices recorded by a change are undone as well
        ctx.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.between(1801L, 1899L)
                .or(RESERVATION_NIGHT.RESERVATION_ID.between(3301L, 3399L))).execute();
        originalNights.forEach(night -> night.changed(true));
        ctx.batchInsert(originalNights).execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-001", "BR-004", "BR-005", "BR-006"})
    void receptionist_changes_dates_room_type_and_number_of_guests() {
        ReservationRecord before = reservation("CHANGE0001");
        logIn("rita");

        // Step 1: the reservation found with UC-007
        navigate(SearchReservationsView.class);
        test($(TextField.class).withCaption("Reservation code").single()).setValue("CHANGE0001");
        test($(Button.class).withText("Search").single()).click();
        test($(Grid.class).single()).select(0);
        test($(Button.class).withText("Change").single()).click();
        assertThat(getCurrentView()).isInstanceOf(ChangeReservationView.class);

        // Step 2: status, dates, room type, number of guests, total price and the data that can be changed
        assertThat(field("status")).isEqualTo("Status: Confirmed");
        assertThat(field("arrival-date")).isEqualTo("Arrival date: May 10, 2031");
        assertThat(field("departure-date")).isEqualTo("Departure date: May 12, 2031");
        assertThat(field("room-type")).isEqualTo("Room type: Double Room");
        assertThat(field("number-of-guests")).isEqualTo("Number of guests: 2");
        assertThat(field("total-price")).isEqualTo("Total price: CHF 300.00");
        assertThat(arrivalDate().isReadOnly()).isFalse();
        assertThat(departureDate().isReadOnly()).isFalse();
        assertThat(roomType().isReadOnly()).isFalse();
        assertThat(numberOfGuests().isReadOnly()).isFalse();

        // Step 3
        test(departureDate()).setValue(LocalDate.of(2031, 5, 13));
        selectRoomType("Family Room");
        test(numberOfGuests()).setValue(3);
        // Step 4
        checkChange();

        // Step 5: old and new total price, 3 nights at 250.00
        assertThat(field("old-total-price")).isEqualTo("Old total price: CHF 300.00");
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 750.00");

        // Step 6
        confirmChange();

        // Step 9: the changed reservation is displayed
        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat(notificationText()).isEqualTo(ChangeReservationView.CHANGED);
        assertThat(field("departure-date")).isEqualTo("Departure date: May 13, 2031");
        assertThat(field("room-type")).isEqualTo("Room type: Family Room");
        assertThat(field("total-price")).isEqualTo("Total price: CHF 750.00");

        // Step 7 / success postconditions: same code, guest and channel
        ReservationRecord after = reservation("CHANGE0001");
        assertThat(after.getArrivalDate()).isEqualTo(LocalDate.of(2031, 5, 10));
        assertThat(after.getDepartureDate()).isEqualTo(LocalDate.of(2031, 5, 13));
        assertThat(after.getRoomTypeId()).isEqualTo(1003L);
        assertThat(after.getNumberOfGuests()).isEqualTo(3);
        assertThat(after.getTotalPrice()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(after.getGuestId()).isEqualTo(before.getGuestId());
        assertThat(after.getChannel()).isEqualTo(before.getChannel());
        assertThat(after.getStatus()).isEqualTo("CONFIRMED");
        // BR-007: the arrival date did not change, so the online cancellation deadline stays
        assertThat(after.getOnlineCancellationDeadline()).isEqualTo(LocalDateTime.of(2031, 5, 8, 14, 0));

        // Step 8: change confirmation email with the code, the new values and, for a Confirmed reservation, the
        // online cancellation deadline (UC-003 BR-006)
        assertThat(EMAIL_SERVICE.sentEmails()).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("clemens.change@example.com");
            assertThat(email.body()).contains("Reservation code: CHANGE0001", "Arrival date: 2031-05-10",
                    "Departure date: 2031-05-13", "Room type: Family Room", "Number of guests: 3",
                    "Total price: CHF 750.00", "Free online cancellation before 8 May 2031, 14:00");
        });
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-004"})
    void availability_reflects_the_changed_reservation() {
        // Before: the Double Room has one free room for the night of 10 May 2031, the Family Room one
        assertThat(availableRooms(LocalDate.of(2031, 5, 10), LocalDate.of(2031, 5, 11), "Double Room")).isEqualTo(1);
        openChange("CHANGE0001");
        selectRoomType("Family Room");
        checkChange();
        confirmChange();

        // Nights no longer reserved are available again, added nights are reserved
        assertThat(availableRooms(LocalDate.of(2031, 5, 10), LocalDate.of(2031, 5, 11), "Double Room")).isEqualTo(2);
        assertThat(availableRooms(LocalDate.of(2031, 5, 10), LocalDate.of(2031, 5, 11), "Family Room")).isZero();
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-005"})
    void kept_night_keeps_its_price_and_the_added_night_gets_the_current_nightly_rate() {
        // Reserved at an older nightly rate: 1 night for 120.00
        openChange("CHANGE0002");

        test(departureDate()).setValue(LocalDate.of(2031, 5, 17));
        checkChange();

        // The night of 15 May keeps 120.00, the added night of 16 May gets the current 150.00
        assertThat(field("old-total-price")).isEqualTo("Old total price: CHF 120.00");
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 270.00");
        confirmChange();
        assertThat(reservation("CHANGE0002").getTotalPrice()).isEqualByComparingTo(new BigDecimal("270.00"));
        assertThat(nightPrices("CHANGE0002")).containsExactly(new BigDecimal("120.00"), new BigDecimal("150.00"));
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-005"})
    void added_nights_in_a_season_get_the_season_rate_and_kept_nights_keep_their_price() {
        // RIAD000003: 3 and 4 November at 90.00; Autumn 2030 (5 to 7 November) sets 160.00 for the Riad (V121)
        openChange("RIAD000003");

        test(departureDate()).setValue(LocalDate.of(2030, 11, 9));
        checkChange();

        // 90.00 + 90.00 + 3 x 160.00 + 100.00 (8 November, normal rate)
        assertThat(field("old-total-price")).isEqualTo("Old total price: CHF 180.00");
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 760.00");
        confirmChange();
        assertThat(nightPrices("RIAD000003")).containsExactly(new BigDecimal("90.00"), new BigDecimal("90.00"),
                new BigDecimal("160.00"), new BigDecimal("160.00"), new BigDecimal("160.00"), new BigDecimal("100.00"));
        assertThat(reservation("RIAD000003").getTotalPrice()).isEqualByComparingTo("760.00");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-005"})
    void nights_no_longer_part_of_the_reservation_are_removed() {
        openChange("RIAD000003");

        // The arrival moves from 3 to 4 November: the night of 3 November is removed, 4 November keeps 90.00
        test(arrivalDate()).setValue(LocalDate.of(2030, 11, 4));
        checkChange();
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 90.00");
        confirmChange();

        assertThat(nightPrices("RIAD000003")).containsExactly(new BigDecimal("90.00"));
        assertThat(reservation("RIAD000003").getTotalPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-005"})
    void every_night_gets_the_current_nightly_rate_after_a_change_of_the_room_type() {
        // RIAD000003: 3 and 4 November at 90.00 in the Riad
        openChange("RIAD000003");

        selectRoomType("Double Room");
        checkChange();

        // Both nights at the current 150.00 of the Double Room
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 300.00");
        confirmChange();
        assertThat(nightPrices("RIAD000003")).containsExactly(new BigDecimal("150.00"), new BigDecimal("150.00"));
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-007"})
    void new_arrival_date_sets_the_48_hour_deadline_also_for_a_reservation_with_24_hours() {
        // CHANGE0002 (arrival 15 May) was recorded with a 24-hour deadline: 14 May, 14:00
        openChange("CHANGE0002");

        test(arrivalDate()).setValue(LocalDate.of(2031, 5, 18));
        test(departureDate()).setValue(LocalDate.of(2031, 5, 19));
        checkChange();
        confirmChange();

        // 48 hours before 14:00 on the new arrival date
        assertThat(reservation("CHANGE0002").getOnlineCancellationDeadline())
                .isEqualTo(LocalDateTime.of(2031, 5, 16, 14, 0));
        assertThat(EMAIL_SERVICE.sentEmails().getFirst().body())
                .contains("Free online cancellation before 16 May 2031, 14:00");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-007"})
    void change_of_only_the_departure_date_keeps_the_24_hour_deadline() {
        openChange("CHANGE0002");

        test(departureDate()).setValue(LocalDate.of(2031, 5, 17));
        checkChange();
        confirmChange();

        assertThat(reservation("CHANGE0002").getOnlineCancellationDeadline())
                .isEqualTo(LocalDateTime.of(2031, 5, 14, 14, 0));
        assertThat(EMAIL_SERVICE.sentEmails().getFirst().body())
                .contains("Free online cancellation before 14 May 2031, 14:00");
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-004"})
    void reservation_keeps_the_nights_it_already_holds() {
        // CHANGE0003 holds the only Family Room for 20 and 21 May; one night later and one more night
        openChange("CHANGE0003");

        test(arrivalDate()).setValue(LocalDate.of(2031, 5, 21));
        test(departureDate()).setValue(LocalDate.of(2031, 5, 24));
        checkChange();

        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 750.00");
        confirmChange();
        assertThat(reservation("CHANGE0003").getArrivalDate()).isEqualTo(LocalDate.of(2031, 5, 21));
    }

    @Test
    @UseCase(id = "UC-005", businessRules = {"BR-004"})
    void reservation_of_an_inactive_room_type_can_keep_it_and_change_its_dates() {
        openChange("CHANGE0006");

        // The inactive Studio is offered only to its own reservation; the inactive Suite is not offered
        assertThat(roomTypeNames()).contains("Studio").doesNotContain("Suite");

        test(departureDate()).setValue(LocalDate.of(2031, 5, 7));
        checkChange();
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 240.00");
        confirmChange();
        assertThat(reservation("CHANGE0006").getRoomTypeId()).isEqualTo(1801L);
        assertThat(reservation("CHANGE0006").getDepartureDate()).isEqualTo(LocalDate.of(2031, 5, 7));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Reservation cannot be changed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A1: Reservation Cannot Be Changed", businessRules = {"BR-001"})
    void cancelled_reservation_is_displayed_read_only() {
        logIn("rita");
        navigate(ReservationView.class, "CHANGE0005");
        assertThat($(Button.class).withText("Change").exists()).isFalse();

        navigate(ChangeReservationView.class, "CHANGE0005");

        assertThat(field("status")).isEqualTo("Status: Cancelled");
        assertThat(errorMessage()).isEqualTo("A reservation with status Cancelled cannot be changed.");
        assertThat($(DatePicker.class).exists()).isFalse();
        assertThat($(Button.class).withText("Check change").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A1: Reservation Cannot Be Changed", businessRules = {"BR-001"})
    void service_refuses_to_change_a_reservation_whose_status_does_not_allow_it() {
        ReservationRecord cancelled = reservation("CHANGE0005");
        assertThatThrownBy(() -> changeService.change("CHANGE0005",
                        new ReservationChange(LocalDate.of(2031, 5, 1),
                                LocalDate.of(2031, 5, 3), 1002L, 2)))
                .isInstanceOf(ReservationNotChangeableException.class);
        assertThat(reservation("CHANGE0005").getDepartureDate()).isEqualTo(cancelled.getDepartureDate());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Guest already checked in
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A2: Guest Already Checked In", businessRules = {"BR-001", "BR-003", "BR-005"})
    void checked_in_guest_extends_the_stay_and_keeps_the_room() {
        openChange("CHANGE0010");

        // A2 step 1: only the departure date can be changed, the assigned room is displayed
        assertThat(field("room")).isEqualTo("Room: 501");
        assertThat(arrivalDate().isReadOnly()).isTrue();
        assertThat(roomType().isReadOnly()).isTrue();
        assertThat(numberOfGuests().isReadOnly()).isTrue();
        assertThat(departureDate().isReadOnly()).isFalse();

        // A2 step 2: two more nights (3 and 4 June)
        test(departureDate()).setValue(LocalDate.of(2030, 6, 5));
        checkChange();

        // BR-005: 6 nights at the reserved 480.00 / 4 nights = 120.00
        assertThat(field("old-total-price")).isEqualTo("Old total price: CHF 480.00");
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 720.00");
        confirmChange();

        ReservationRecord after = reservation("CHANGE0010");
        assertThat(after.getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 5));
        assertThat(after.getTotalPrice()).isEqualByComparingTo(new BigDecimal("720.00"));
        // The guest keeps the assigned room
        assertThat(after.getRoomId()).isEqualTo(1801L);
        assertThat(after.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(after.getOnlineCancellationDeadline())
                .isEqualTo(reservationBefore("CHANGE0010").getOnlineCancellationDeadline());
        // Step 8: the online cancellation deadline is shown only for a Confirmed reservation
        assertThat(EMAIL_SERVICE.sentEmails()).singleElement().satisfies(email ->
                assertThat(email.body()).contains("Departure date: 2030-06-05")
                        .doesNotContain("online cancellation", "cancelled online"));
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A2: Guest Already Checked In", businessRules = {"BR-005"})
    void checked_in_guest_keeps_the_prices_of_the_reserved_nights_and_pays_the_current_rate_for_added_nights() {
        // RIAD000002 is checked in to R02 until 4 June: 100.00, 2 x 180.00 (Late May 2030 season), 3 x 100.00
        openChange("RIAD000002");

        test(departureDate()).setValue(LocalDate.of(2030, 6, 6));
        checkChange();

        // 760.00 + 2 added nights at the current normal rate of 100.00
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 960.00");
        confirmChange();
        assertThat(nightPrices("RIAD000002")).containsExactly(new BigDecimal("100.00"), new BigDecimal("180.00"),
                new BigDecimal("180.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("100.00"));
        assertThat(reservation("RIAD000002").getTotalPrice()).isEqualByComparingTo("960.00");
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A2: Guest Already Checked In", businessRules = {"BR-003", "BR-005"})
    void checked_in_guest_shortens_the_stay() {
        openChange("CHANGE0010");

        test(departureDate()).setValue(LocalDate.of(2030, 6, 2));
        checkChange();

        // 3 nights at 120.00
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 360.00");
        confirmChange();
        assertThat(reservation("CHANGE0010").getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 2));
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A2: Guest Already Checked In", businessRules = {"BR-003"})
    void checked_in_guest_cannot_leave_today_with_a_change() {
        openChange("CHANGE0010");

        // Leaving today is a check-out (UC-010)
        test(departureDate()).setValue(LocalDate.of(2030, 6, 1));
        checkChange();

        assertThat(departureDate().isInvalid()).isTrue();
        assertThat(departureDate().getErrorMessage()).isEqualTo("The departure date must be after today");
        assertThat($(Paragraph.class).withId("new-total-price").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A4: Not Available for the New Values", businessRules = {"BR-003"})
    void checked_in_guest_cannot_extend_when_the_assigned_room_is_blocked() {
        openChange("CHANGE0010");

        // Room 501 is blocked for the night of 5 June, another Studio would still be free
        test(departureDate()).setValue(LocalDate.of(2030, 6, 6));
        checkChange();

        assertThat(errorMessage()).isEqualTo("The room type is not available for these nights: Jun 5, 2030.");
        assertThat(reservation("CHANGE0010").getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 3));
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A4: Not Available for the New Values", businessRules = {"BR-003"})
    void checked_in_guest_is_told_every_night_that_is_not_available() {
        openChange("CHANGE0010");

        // 5 June: room 501 blocked; 7 June: both Studios reserved by other guests
        test(departureDate()).setValue(LocalDate.of(2030, 6, 9));
        checkChange();

        assertThat(errorMessage())
                .isEqualTo("The room type is not available for these nights: Jun 5, 2030, Jun 7, 2030.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Invalid new values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A3: Invalid New Values", businessRules = {"BR-002"})
    void arrival_date_in_the_past_or_more_than_365_days_ahead_is_marked_invalid() {
        openChange("CHANGE0001");

        test(arrivalDate()).setValue(LocalDate.of(2030, 5, 31));
        checkChange();
        assertThat(arrivalDate().isInvalid()).isTrue();
        assertThat(arrivalDate().getErrorMessage()).isEqualTo("The arrival date must be between today and 365 days ahead");

        test(arrivalDate()).setValue(LocalDate.of(2031, 6, 2));
        test(departureDate()).setValue(LocalDate.of(2031, 6, 3));
        checkChange();
        assertThat(arrivalDate().isInvalid()).isTrue();
        assertThat($(Paragraph.class).withId("new-total-price").exists()).isFalse();

        // A3 step 2: continues at step 3
        test(arrivalDate()).setValue(LocalDate.of(2031, 5, 11));
        test(departureDate()).setValue(LocalDate.of(2031, 5, 12));
        checkChange();
        assertThat(arrivalDate().isInvalid()).isFalse();
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 150.00");
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A3: Invalid New Values", businessRules = {"BR-002"})
    void departure_date_not_after_arrival_date_is_marked_invalid() {
        openChange("CHANGE0001");

        test(departureDate()).setValue(LocalDate.of(2031, 5, 10));
        checkChange();

        assertThat(departureDate().isInvalid()).isTrue();
        assertThat(departureDate().getErrorMessage()).isEqualTo("The departure date must be after the arrival date");
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A3: Invalid New Values")
    void number_of_guests_outside_1_to_10_or_above_the_room_type_maximum_is_marked_invalid() {
        openChange("CHANGE0001");

        test(numberOfGuests()).setValue(0);
        checkChange();
        assertThat(numberOfGuests().isInvalid()).isTrue();
        assertThat(numberOfGuests().getErrorMessage()).isEqualTo("The number of guests must be between 1 and 10");

        test(numberOfGuests()).setValue(11);
        checkChange();
        assertThat(numberOfGuests().isInvalid()).isTrue();

        // The Double Room takes at most 2 guests
        test(numberOfGuests()).setValue(3);
        checkChange();
        assertThat(numberOfGuests().isInvalid()).isTrue();
        assertThat(numberOfGuests().getErrorMessage()).isEqualTo("The Double Room takes at most 2 guests");
        assertThat(reservation("CHANGE0001").getNumberOfGuests()).isEqualTo(2);
    }

    @Test
    @UseCase(id = "UC-005", scenario = "A3: Invalid New Values", businessRules = {"BR-004"})
    void reservation_cannot_be_moved_to_an_inactive_room_type() {
        openChange("CHANGE0001");

        // The inactive room types are not offered
        assertThat(roomTypeNames()).containsExactly("Single Room", "Double Room", "Family Room");

        // Moving to an inactive room type is refused also when asked for directly
        assertThatThrownBy(() -> changeService.check("CHANGE0001",
                        new ReservationChange(LocalDate.of(2031, 5, 10),
                                LocalDate.of(2031, 5, 12), 1004L, 2)))
                .isInstanceOf(InvalidChangeException.class)
                .hasMessage("Select an active room type");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Not available for the new values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A4: Not Available for the New Values", businessRules = {"BR-004"})
    void receptionist_is_told_which_nights_are_not_available() {
        openChange("CHANGE0003");

        // Another guest has the only Family Room for the night of 25 May
        test(departureDate()).setValue(LocalDate.of(2031, 5, 27));
        checkChange();

        assertThat(errorMessage()).isEqualTo("The room type is not available for these nights: May 25, 2031.");
        assertThat($(Paragraph.class).withId("new-total-price").exists()).isFalse();
        // The reservation stays unchanged
        assertThat(reservation("CHANGE0003").getDepartureDate()).isEqualTo(LocalDate.of(2031, 5, 22));

        // A4 step 2: continues at step 3
        test(departureDate()).setValue(LocalDate.of(2031, 5, 25));
        checkChange();
        assertThat(field("new-total-price")).isEqualTo("New total price: CHF 1250.00");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Nothing changed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A5: Nothing Changed")
    void confirming_without_a_change_keeps_the_reservation_and_sends_no_email() {
        ReservationRecord before = reservation("CHANGE0002");
        openChange("CHANGE0002");

        checkChange();
        confirmChange();

        // A5 step 2: the use case ends with the reservation displayed
        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat(notificationText()).isEqualTo(ChangeReservationView.NOTHING_CHANGED);
        // A5 step 1: unchanged, even though the nightly rate has changed since it was reserved
        assertThat(reservation("CHANGE0002")).isEqualTo(before);
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A6: Receptionist discards the change
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A6: Receptionist Discards the Change")
    void entered_values_are_discarded_when_the_receptionist_leaves() {
        ReservationRecord before = reservation("CHANGE0001");
        openChange("CHANGE0001");
        test(departureDate()).setValue(LocalDate.of(2031, 5, 14));
        checkChange();

        test($(Button.class).withText("Discard").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat(field("departure-date")).isEqualTo("Departure date: May 12, 2031");
        assertThat(reservation("CHANGE0001")).isEqualTo(before);
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();

        // Opening the change again shows the reservation's values
        test($(Button.class).withText("Change").single()).click();
        assertThat(departureDate().getValue()).isEqualTo(LocalDate.of(2031, 5, 12));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A7: No longer available at confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A7: No Longer Available at Confirmation", businessRules = {"BR-004"})
    void receptionist_is_informed_when_another_reservation_took_the_last_room_since_the_check() {
        // The first receptionist extends CHANGE0003 by the night of 22 May (a view of its own, standing in for
        // their browser)
        logIn("rita");
        ChangeReservationView firstView = new ChangeReservationView(changeService);
        UI.getCurrent().add(firstView);
        firstView.setParameter(null, "CHANGE0003");
        test($(DatePicker.class, firstView).withCaption("Departure date").single())
                .setValue(LocalDate.of(2031, 5, 23));
        test($(Button.class, firstView).withText("Check change").single()).click();
        assertThat($(Paragraph.class, firstView).withId("new-total-price").single().getText())
                .isEqualTo("New total price: CHF 750.00");

        // Meanwhile another receptionist moves CHANGE0002 to the only Family Room for that night
        navigate(ChangeReservationView.class, "CHANGE0002");
        test(arrivalDate()).setValue(LocalDate.of(2031, 5, 22));
        test(departureDate()).setValue(LocalDate.of(2031, 5, 23));
        selectRoomType("Family Room");
        checkChange();
        confirmChange();
        assertThat(reservation("CHANGE0002").getRoomTypeId()).isEqualTo(1003L);

        test($(Button.class, firstView).withText("Confirm change").single()).click();

        // A7 step 1: informed, the reservation stays unchanged
        assertThat($(Paragraph.class, firstView).withId("error-message").single().getText())
                .isEqualTo("The room type is no longer available for these nights: May 22, 2031.");
        assertThat(reservation("CHANGE0003").getDepartureDate()).isEqualTo(LocalDate.of(2031, 5, 22));
        // No overbooking: only one Family Room reservation for the night of 22 May
        assertThat(availableRooms(LocalDate.of(2031, 5, 22), LocalDate.of(2031, 5, 23), "Family Room")).isZero();
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_TYPE_ID.eq(1003L)
                .and(RESERVATION.ARRIVAL_DATE.le(LocalDate.of(2031, 5, 22)))
                .and(RESERVATION.DEPARTURE_DATE.gt(LocalDate.of(2031, 5, 22)))
                .and(RESERVATION.STATUS.eq("CONFIRMED")))).isEqualTo(1);
        assertThat(EMAIL_SERVICE.sentEmails()).hasSize(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A8: Email Service does not accept the change confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005", scenario = "A8: Email Service Does Not Accept the Change Confirmation",
            businessRules = {"BR-006"})
    void change_is_kept_when_the_email_service_refuses_the_email() {
        EMAIL_SERVICE.setRefusing(true);
        openChange("CHANGE0001");
        test(departureDate()).setValue(LocalDate.of(2031, 5, 13));
        checkChange();

        confirmChange();

        // A8 step 2: continues at step 9
        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat(field("departure-date")).isEqualTo("Departure date: May 13, 2031");
        // A8 step 1: the change is kept
        assertThat(reservation("CHANGE0001").getDepartureDate()).isEqualTo(LocalDate.of(2031, 5, 13));
        assertThat(EMAIL_SERVICE.sentEmails()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-005")
    void only_a_logged_in_receptionist_can_change_a_reservation() {
        navigate("reservation-change/CHANGE0001", LoginView.class);

        logIn("hanna");
        assertThat(navigate("reservation-change/CHANGE0001", RouteNotFoundError.class)).isNotNull();
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

    private void openChange(String reservationCode) {
        logIn("rita");
        navigate(ChangeReservationView.class, reservationCode);
    }

    private DatePicker arrivalDate() {
        return $view(DatePicker.class).withCaption("Arrival date").single();
    }

    private DatePicker departureDate() {
        return $view(DatePicker.class).withCaption("Departure date").single();
    }

    @SuppressWarnings("unchecked")
    private Select<RoomTypeChoice> roomType() {
        return $view(Select.class).withCaption("Room type").single();
    }

    private IntegerField numberOfGuests() {
        return $view(IntegerField.class).withCaption("Number of guests").single();
    }

    private void selectRoomType(String name) {
        test(roomType()).selectItem(name);
    }

    private List<String> roomTypeNames() {
        return roomType().getListDataView().getItems().map(RoomTypeChoice::name).toList();
    }

    private void checkChange() {
        test($view(Button.class).withText("Check change").single()).click();
    }

    private void confirmChange() {
        test($view(Button.class).withText("Confirm change").single()).click();
    }

    private String field(String id) {
        return $view(Paragraph.class).withId(id).single().getText();
    }

    private String errorMessage() {
        return field("error-message");
    }

    private String notificationText() {
        return test($(Notification.class).last()).getText();
    }

    private long availableRooms(LocalDate arrival, LocalDate departure, String roomTypeName) {
        return receptionReservationService.searchAvailableRoomTypes(arrival, departure, 1).stream()
                .filter(roomType -> roomType.name().equals(roomTypeName))
                .mapToLong(ReceptionRoomType::availableRooms)
                .findFirst()
                .orElse(0);
    }

    private ReservationRecord reservationBefore(String reservationCode) {
        return originalReservations.stream()
                .filter(original -> original.getReservationCode().equals(reservationCode))
                .findFirst()
                .orElseThrow();
    }

    private List<BigDecimal> nightPrices(String reservationCode) {
        return ctx.select(RESERVATION_NIGHT.NIGHT_PRICE).from(RESERVATION_NIGHT)
                .join(RESERVATION).on(RESERVATION.ID.eq(RESERVATION_NIGHT.RESERVATION_ID))
                .where(RESERVATION.RESERVATION_CODE.eq(reservationCode))
                .orderBy(RESERVATION_NIGHT.NIGHT_DATE)
                .fetch(RESERVATION_NIGHT.NIGHT_PRICE);
    }

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }
}
