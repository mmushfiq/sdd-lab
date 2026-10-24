package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reception.CheckInService;
import ch.martinelli.edu.hrs.reception.RoomCandidate;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-009 "Check In Guest". Test data: V109__uc009_test_data.sql (room types Cabin with rooms 701 to
 * 706 and Hut with rooms 801 and 802), staff members from V103__uc022_test_data.sql (password Secret-123); now is
 * 1 June 2030, 12:00 hotel time, before the check-in time of 14:00.
 */
@WithAnonymousUser
class UC009CheckInGuestTest extends HrsBrowserlessTest {

    @TestBean
    Clock clock;

    @Autowired
    CheckInService checkInService;

    @Autowired
    DSLContext ctx;

    private Result<ReservationRecord> originalReservations;
    private Result<RoomRecord> originalRooms;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalReservations = ctx.selectFrom(RESERVATION).where(RESERVATION.ID.between(2101L, 2199L)).fetch();
        originalRooms = ctx.selectFrom(ROOM).where(ROOM.ID.between(2101L, 2199L)).fetch();
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
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", businessRules = {"BR-001", "BR-002", "BR-004"})
    void receptionist_checks_in_a_guest_to_the_proposed_room() {
        logIn("rita");

        // Step 1: the reservation from today's arrivals (UC-008)
        Grid<?> arrivals = $(Grid.class).withId("arrivals").single();
        int row = rowOf(arrivals, "CHECKIN001");
        test(arrivals).select(row);

        // Step 2
        test($(Button.class).withText("Check in").single()).click();
        assertThat(getCurrentView()).isInstanceOf(CheckInView.class);

        // Step 4: clean, active, free rooms of the room type not blocked on a night of the stay; 703 is dirty,
        // 704 blocked on 2 June, 705 inactive and 706 occupied
        RadioButtonGroup<RoomCandidate> rooms = roomChoice();
        assertThat(roomNumbers(rooms)).containsExactly("701", "702");
        assertThat(rooms.getValue().roomNumber()).isEqualTo("701");

        // Step 5: the receptionist confirms the proposed room
        checkIn();

        // Step 8: the room number
        assertThat(field("checked-in-room")).isEqualTo("Room: 701");

        // Step 7 / success postconditions
        ReservationRecord reservation = reservation("CHECKIN001");
        assertThat(reservation.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(reservation.getRoomId()).isEqualTo(2101L);
        assertThat(reservation.getCheckedInAt()).isEqualTo(LocalDateTime.now(clock));
        // BR-004: the cleaning status is not changed
        assertThat(room(2101L).getCleaningStatus()).isEqualTo("CLEAN");
    }

    @Test
    @UseCase(id = "UC-009", businessRules = {"BR-002"})
    void receptionist_selects_another_listed_room() {
        openCheckIn("CHECKIN001");

        test(roomChoice()).selectItem("702");
        checkIn();

        assertThat(field("checked-in-room")).isEqualTo("Room: 702");
        assertThat(reservation("CHECKIN001").getRoomId()).isEqualTo(2102L);
    }

    @Test
    @UseCase(id = "UC-009", businessRules = {"BR-002"})
    void assigned_room_is_no_longer_offered_to_other_check_ins() {
        openCheckIn("CHECKIN001");
        checkIn();

        openCheckIn("CHECKIN002");

        // 704 is free for the night of 1 June: the block is on 2 June only
        assertThat(roomNumbers(roomChoice())).containsExactly("702", "704");
        assertThat(roomChoice().getValue().roomNumber()).isEqualTo("702");
    }

    @Test
    @UseCase(id = "UC-009")
    void checked_in_reservation_shows_the_assigned_room() {
        openCheckIn("CHECKIN001");
        checkIn();

        test($(Button.class).withText("Show reservation").single()).click();

        assertThat(field("status")).isEqualTo("Status: Checked In");
        assertThat(field("room")).isEqualTo("Room: 701");
        assertThat($(Button.class).withText("Check in").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Reservation not confirmed
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", scenario = "A1: Reservation Not Confirmed")
    void cancelled_reservation_cannot_be_checked_in() {
        openCheckIn("CHECKIN004");

        assertThat(field("error-message")).isEqualTo("A reservation with status Cancelled cannot be checked in.");
        assertThat($(RadioButtonGroup.class).exists()).isFalse();
        assertThat(reservation("CHECKIN004").getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @UseCase(id = "UC-009", scenario = "A1: Reservation Not Confirmed")
    void checked_in_reservation_cannot_be_checked_in_again() {
        openCheckIn("CHECKIN001");
        checkIn();

        navigate(CheckInView.class, "CHECKIN001");

        assertThat(field("error-message")).isEqualTo("A reservation with status Checked In cannot be checked in.");
        assertThat($(RadioButtonGroup.class).exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Not the arrival date
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", scenario = "A2: Not the Arrival Date", businessRules = {"BR-001"})
    void reservation_can_be_checked_in_only_on_its_arrival_date() {
        logIn("rita");
        navigate(ReservationView.class, "CHECKIN003");
        test($(Button.class).withText("Check in").single()).click();

        assertThat(field("error-message")).isEqualTo(CheckInView.NOT_ARRIVAL_DATE);
        assertThat($(RadioButtonGroup.class).exists()).isFalse();
        assertThat(reservation("CHECKIN003").getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: No room ready
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", scenario = "A3: No Room Ready", businessRules = {"BR-002"})
    void receptionist_is_informed_when_no_clean_room_is_free_and_sees_the_dirty_ones() {
        openCheckIn("CHECKIN005");

        // 802 is occupied, 801 is free but dirty
        assertThat(field("error-message")).isEqualTo("No clean room of the room type Hut is free.");
        assertThat(field("dirty-rooms")).isEqualTo("Free rooms that are still dirty: 801.");
        assertThat($(RadioButtonGroup.class).exists()).isFalse();
        assertThat(reservation("CHECKIN005").getStatus()).isEqualTo("CONFIRMED");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Room no longer assignable
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", scenario = "A4: Room No Longer Assignable", businessRules = {"BR-003"})
    void receptionist_is_informed_when_another_receptionist_assigned_the_room_meanwhile() {
        logIn("rita");
        // The first receptionist's check-in, in a view of its own standing in for their browser
        CheckInView firstView = new CheckInView(checkInService);
        UI.getCurrent().add(firstView);
        firstView.setParameter(null, "CHECKIN001");
        assertThat(roomChoice(firstView).getValue().roomNumber()).isEqualTo("701");

        // Another receptionist checks in CHECKIN002 to the proposed room 701 first
        navigate(CheckInView.class, "CHECKIN002");
        checkIn();
        assertThat(field("checked-in-room")).isEqualTo("Room: 701");

        test($(Button.class, firstView).withText("Check in").single()).click();

        // A4 step 1: informed; step 2: continues at step 4 with the rooms that can still be assigned
        assertThat($(Paragraph.class, firstView).withId("error-message").single().getText())
                .isEqualTo("Room 701 can no longer be assigned. Please select another room.");
        assertThat(roomNumbers(roomChoice(firstView))).containsExactly("702");
        // Failure postcondition: the room is assigned to only one checked-in reservation
        assertThat(reservation("CHECKIN001").getStatus()).isEqualTo("CONFIRMED");
        assertThat(reservation("CHECKIN001").getRoomId()).isNull();
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(2101L)
                .and(RESERVATION.STATUS.eq("CHECKED_IN")))).isEqualTo(1);

        test($(Button.class, firstView).withText("Check in").single()).click();
        assertThat($(Paragraph.class, firstView).withId("checked-in-room").single().getText()).isEqualTo("Room: 702");
    }

    @Test
    @UseCase(id = "UC-009", scenario = "A4: Room No Longer Assignable", businessRules = {"BR-002"})
    void room_set_to_dirty_since_it_was_listed_can_no_longer_be_assigned() {
        openCheckIn("CHECKIN001");

        // Meanwhile room 701 is set to dirty
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ID.eq(2101L)).execute();
        checkIn();

        assertThat(field("error-message")).isEqualTo("Room 701 can no longer be assigned. Please select another room.");
        assertThat(roomNumbers(roomChoice())).containsExactly("702");
        assertThat(reservation("CHECKIN001").getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @UseCase(id = "UC-009", scenario = "A4: Room No Longer Assignable", businessRules = {"BR-002"})
    void room_blocked_since_it_was_listed_can_no_longer_be_assigned() {
        openCheckIn("CHECKIN001");

        // Meanwhile room 701 is blocked for the night of 2 June
        ctx.execute("INSERT INTO room_block (id, room_id, start_date, end_date, reason) "
                + "VALUES (2199, 2101, DATE '2030-06-02', DATE '2030-06-02', 'Leak')");
        try {
            checkIn();

            assertThat(field("error-message"))
                    .isEqualTo("Room 701 can no longer be assigned. Please select another room.");
            assertThat(reservation("CHECKIN001").getRoomId()).isNull();
        } finally {
            ctx.execute("DELETE FROM room_block WHERE id = 2199");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Receptionist stops the check-in
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009", scenario = "A5: Receptionist Stops the Check-in")
    void reservation_stays_unchanged_when_the_receptionist_leaves_the_check_in() {
        ReservationRecord before = reservation("CHECKIN001");
        openCheckIn("CHECKIN001");

        test($(Button.class).withText("Cancel").single()).click();

        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat(reservation("CHECKIN001")).isEqualTo(before);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-009")
    void only_a_logged_in_receptionist_can_check_in_a_guest() {
        navigate("check-in/CHECKIN001", LoginView.class);

        logIn("hanna");
        assertThat(navigate("check-in/CHECKIN001", RouteNotFoundError.class)).isNotNull();
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

    private void openCheckIn(String reservationCode) {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !(SecurityContextHolder.getContext().getAuthentication().getName().equals("rita"))) {
            logIn("rita");
        }
        navigate(CheckInView.class, reservationCode);
    }

    private void checkIn() {
        test($view(Button.class).withText("Check in").single()).click();
    }

    @SuppressWarnings("unchecked")
    private RadioButtonGroup<RoomCandidate> roomChoice() {
        return $view(RadioButtonGroup.class).withCaption("Room").single();
    }

    @SuppressWarnings("unchecked")
    private RadioButtonGroup<RoomCandidate> roomChoice(Component view) {
        return $(RadioButtonGroup.class, view).withCaption("Room").single();
    }

    private static List<String> roomNumbers(RadioButtonGroup<RoomCandidate> rooms) {
        return rooms.getListDataView().getItems().map(RoomCandidate::roomNumber).toList();
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

    private ReservationRecord reservation(String reservationCode) {
        return ctx.selectFrom(RESERVATION).where(RESERVATION.RESERVATION_CODE.eq(reservationCode)).fetchSingle();
    }

    private RoomRecord room(Long roomId) {
        return ctx.selectFrom(ROOM).where(ROOM.ID.eq(roomId)).fetchSingle();
    }
}
