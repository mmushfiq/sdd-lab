package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationNightRecord;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reception.CheckOutService;
import ch.martinelli.edu.hrs.reception.RoomAssignmentRepository;
import ch.martinelli.edu.hrs.reception.RoomCandidate;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION_NIGHT;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-010 "Check Out Guest". Test data: V110__uc010_test_data.sql (room type Chalet with rooms 901
 * to 904), staff members from V103__uc022_test_data.sql (password Secret-123); now is 1 June 2030, 12:00 hotel time.
 */
@WithAnonymousUser
class UC010CheckOutGuestTest extends HrsBrowserlessTest {

    @TestBean
    Clock clock;

    @Autowired
    CheckOutService checkOutService;

    @Autowired
    RoomAssignmentRepository roomAssignmentRepository;

    @Autowired
    DSLContext ctx;

    private Result<ReservationRecord> originalReservations;
    private Result<RoomRecord> originalRooms;
    private Result<ReservationNightRecord> originalNights;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalReservations = ctx.selectFrom(RESERVATION).where(RESERVATION.ID.between(2201L, 2299L)
                .or(RESERVATION.ID.between(3301L, 3399L))).fetch();
        originalNights = ctx.selectFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.between(2201L, 2299L)
                .or(RESERVATION_NIGHT.RESERVATION_ID.between(3301L, 3399L))).fetch();
        originalRooms = ctx.selectFrom(ROOM).where(ROOM.ID.between(2201L, 2299L)
                .or(ROOM.ID.between(3301L, 3399L))).fetch();
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        originalReservations.forEach(original -> {
            original.changed(true);
            original.changed(RESERVATION.ID, false);
            ctx.update(RESERVATION).set(original).where(RESERVATION.ID.eq(original.getId())).execute();
        });
        originalRooms.forEach(original -> {
            original.changed(true);
            original.changed(ROOM.ID, false);
            ctx.update(ROOM).set(original).where(ROOM.ID.eq(original.getId())).execute();
        });
        // The night prices recorded by a check-out are undone as well
        ctx.deleteFrom(RESERVATION_NIGHT).where(RESERVATION_NIGHT.RESERVATION_ID.between(2201L, 2299L)
                .or(RESERVATION_NIGHT.RESERVATION_ID.between(3301L, 3399L))).execute();
        originalNights.forEach(night -> night.changed(true));
        ctx.batchInsert(originalNights).execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", businessRules = {"BR-001", "BR-004", "BR-005"})
    void receptionist_checks_out_a_guest_departing_today() {
        logIn("rita");

        // Step 1: the reservation from today's departures (UC-008)
        Grid<?> departures = $(Grid.class).withId("departures").single();
        test(departures).select(rowOf(departures, "CHECKOUT01"));

        // Step 2
        test($(Button.class).withText("Check out").single()).click();
        assertThat(getCurrentView()).isInstanceOf(CheckOutView.class);

        // Step 4: assigned room, arrival date, departure date, number of nights and total price
        assertThat(field("room")).isEqualTo("Room: 901");
        assertThat(field("arrival-date")).isEqualTo("Arrival date: May 29, 2030");
        assertThat(field("departure-date")).isEqualTo("Departure date: Jun 1, 2030");
        assertThat(field("nights")).isEqualTo("Number of nights: 3");
        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 450.00");

        // Step 5
        confirmCheckOut();

        // Step 7: the total price of the stay; BR-004: no payment is recorded
        assertThat(field("total-price")).isEqualTo("Total price of the stay: CHF 450.00");
        assertThat(field("payment-note")).isEqualTo(CheckOutView.NO_PAYMENT_RECORDED);

        // Step 6 / success postconditions
        ReservationRecord reservation = reservation("CHECKOUT01");
        assertThat(reservation.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(reservation.getCheckedOutAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo("450.00");
        // The assigned room stays recorded on the reservation, is free for the next check-in and is dirty
        assertThat(reservation.getRoomId()).isEqualTo(2201L);
        assertThat(room(2201L).getCleaningStatus()).isEqualTo("DIRTY");
        assertThat(roomAssignmentRepository.findFreeRooms(2201L, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 2)))
                .extracting(RoomCandidate::roomNumber).contains("901");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Reservation not checked in
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", scenario = "A1: Reservation Not Checked In", businessRules = {"BR-001"})
    void reservation_that_is_not_checked_in_cannot_be_checked_out() {
        logIn("rita");
        navigate(ReservationView.class, "CHECKOUT05");
        assertThat($(Button.class).withText("Check out").exists()).isFalse();

        navigate(CheckOutView.class, "CHECKOUT05");

        assertThat(field("error-message"))
                .isEqualTo("Only a checked-in guest can be checked out. The reservation has status Confirmed.");
        assertThat($(Button.class).withText("Confirm check-out").exists()).isFalse();
        assertThat(reservation("CHECKOUT05").getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Early check-out
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Early Check-out", businessRules = {"BR-002", "BR-005"})
    void guest_leaving_early_pays_for_the_nights_stayed() {
        openCheckOut("CHECKOUT02");

        // A2 steps 1 and 2: informed, reserved and new departure date and total price
        assertThat(field("check-out-message")).isEqualTo(CheckOutView.EARLY);
        assertThat(field("reserved-departure-date")).isEqualTo("Reserved departure date: Jun 4, 2030");
        assertThat(field("departure-date")).isEqualTo("New departure date: Jun 1, 2030");
        assertThat(field("nights")).isEqualTo("Number of nights: 2");
        assertThat(field("reserved-total-price")).isEqualTo("Reserved total price: CHF 500.00");
        // The 2 nights stayed keep their night prices of 100.00 each
        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 200.00");

        // A2 step 3: continues at step 5
        confirmCheckOut();

        assertThat(field("total-price")).isEqualTo("Total price of the stay: CHF 200.00");
        ReservationRecord reservation = reservation("CHECKOUT02");
        assertThat(reservation.getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 1));
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        // BR-005: dirty regardless of the previous cleaning status
        assertThat(room(2202L).getCleaningStatus()).isEqualTo("DIRTY");
        // The released nights are available again: the reservation no longer covers 1 to 3 June
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_TYPE_ID.eq(2201L)
                .and(RESERVATION.STATUS.in("CONFIRMED", "CHECKED_IN"))
                .and(RESERVATION.DEPARTURE_DATE.gt(LocalDate.of(2030, 6, 2)))
                .and(RESERVATION.ARRIVAL_DATE.le(LocalDate.of(2030, 6, 2))))).isZero();
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Early Check-out", businessRules = {"BR-002"})
    void nights_stayed_keep_their_different_night_prices() {
        // RIAD000002 until 4 June: 29 May 100.00, 30 and 31 May 180.00 (season), 1 to 3 June 100.00 each
        openCheckOut("RIAD000002");

        assertThat(field("reserved-total-price")).isEqualTo("Reserved total price: CHF 760.00");
        assertThat(field("nights")).isEqualTo("Number of nights: 3");
        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 460.00");
        confirmCheckOut();

        // The nights from the new departure date on are removed
        assertThat(nightPrices("RIAD000002")).containsExactly(new BigDecimal("100.00"), new BigDecimal("180.00"),
                new BigDecimal("180.00"));
        assertThat(reservation("RIAD000002").getTotalPrice()).isEqualByComparingTo("460.00");
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A2: Early Check-out", businessRules = {"BR-002"})
    void departure_date_is_at_least_the_day_after_the_arrival_date() {
        // Arrived today for one night and leaves today
        openCheckOut("CHECKOUT04");

        assertThat(field("departure-date")).isEqualTo("New departure date: Jun 2, 2030");
        assertThat(field("nights")).isEqualTo("Number of nights: 1");
        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 150.00");

        confirmCheckOut();
        assertThat(reservation("CHECKOUT04").getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 2));
        assertThat(reservation("CHECKOUT04").getTotalPrice()).isEqualByComparingTo("150.00");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Overdue check-out
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", scenario = "A3: Overdue Check-out", businessRules = {"BR-003"})
    void overdue_guest_pays_for_the_additional_nights() {
        openCheckOut("CHECKOUT03");

        // A3 steps 1 and 2
        assertThat(field("check-out-message")).isEqualTo(CheckOutView.OVERDUE);
        assertThat(field("reserved-departure-date")).isEqualTo("Reserved departure date: May 30, 2030");
        assertThat(field("departure-date")).isEqualTo("New departure date: Jun 1, 2030");
        assertThat(field("nights")).isEqualTo("Number of nights: 5");
        assertThat(field("reserved-total-price")).isEqualTo("Reserved total price: CHF 300.00");
        // The 3 reserved nights keep their 300.00; 30 and 31 May at the current 150.00 of the Chalet
        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 600.00");

        confirmCheckOut();

        ReservationRecord reservation = reservation("CHECKOUT03");
        assertThat(reservation.getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(reservation.getDepartureDate()).isEqualTo(LocalDate.of(2030, 6, 1));
        assertThat(reservation.getTotalPrice()).isEqualByComparingTo("600.00");
        assertThat(nightPrices("CHECKOUT03")).containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("150.00"), new BigDecimal("150.00"));
    }

    @Test
    @UseCase(id = "UC-010", scenario = "A3: Overdue Check-out", businessRules = {"BR-003"})
    void additional_nights_in_a_season_get_the_season_rate() {
        // RIAD000001: 3 nights at 100.00 until 30 May; Late May 2030 sets 180.00 for 30 and 31 May (V121)
        openCheckOut("RIAD000001");

        assertThat(field("total-price")).isEqualTo("Total price to be paid: CHF 660.00");
        confirmCheckOut();

        assertThat(reservation("RIAD000001").getTotalPrice()).isEqualByComparingTo("660.00");
        assertThat(nightPrices("RIAD000001")).containsExactly(new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), new BigDecimal("180.00"), new BigDecimal("180.00"));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Receptionist stops the check-out
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", scenario = "A4: Receptionist Stops the Check-out")
    void reservation_stays_unchanged_when_the_receptionist_does_not_confirm() {
        ReservationRecord before = reservation("CHECKOUT02");
        openCheckOut("CHECKOUT02");

        test($(Button.class).withText("Cancel").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        // Failure postconditions: status, dates, total price, assigned room and cleaning status unchanged
        assertThat(reservation("CHECKOUT02")).isEqualTo(before);
        assertThat(room(2202L).getCleaningStatus()).isEqualTo("DIRTY");
        assertThat(room(2201L).getCleaningStatus()).isEqualTo("CLEAN");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Status changed before confirmation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010", scenario = "A5: Status Changed Before Confirmation")
    void check_out_is_refused_when_someone_else_checked_the_guest_out_meanwhile() {
        openCheckOut("CHECKOUT01");

        // Another receptionist checks the guest out first
        checkOutService.checkOut("CHECKOUT01");
        LocalDateTime firstCheckOut = reservation("CHECKOUT01").getCheckedOutAt();

        confirmCheckOut();

        // A5 step 1: refused, the current status is displayed
        assertThat(field("error-message"))
                .isEqualTo("Only a checked-in guest can be checked out. The reservation has status Checked Out.");
        assertThat(reservation("CHECKOUT01").getCheckedOutAt()).isEqualTo(firstCheckOut);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-010")
    void only_a_logged_in_receptionist_can_check_out_a_guest() {
        navigate("check-out/CHECKOUT01", LoginView.class);

        logIn("hanna");
        assertThat(navigate("check-out/CHECKOUT01", RouteNotFoundError.class)).isNotNull();
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

    private void openCheckOut(String reservationCode) {
        logIn("rita");
        navigate(CheckOutView.class, reservationCode);
    }

    private void confirmCheckOut() {
        test($(Button.class).withText("Confirm check-out").single()).click();
    }

    private int rowOf(Grid<?> grid, String reservationCode) {
        for (int row = 0; row < test(grid).size(); row++) {
            if (test(grid).getCellText(row, 0).equals(reservationCode)) {
                return row;
            }
        }
        throw new AssertionError(reservationCode + " is not listed");
    }

    private String field(String id) {
        return $(Paragraph.class).withId(id).single().getText();
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

    private RoomRecord room(Long roomId) {
        return ctx.selectFrom(ROOM).where(ROOM.ID.eq(roomId)).fetchSingle();
    }
}
