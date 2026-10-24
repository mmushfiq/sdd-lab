package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.housekeeping.CleaningListEntry;
import ch.martinelli.edu.hrs.reception.RoomAssignmentRepository;
import ch.martinelli.edu.hrs.reception.RoomCandidate;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
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
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-014 "Mark Room Clean". Test data: V113__uc013_test_data.sql (room type Pod with rooms C01 to
 * C08); staff members from V103__uc022_test_data.sql (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC014MarkRoomCleanTest extends HrsBrowserlessTest {

    @TestBean
    Clock clock;

    @Autowired
    RoomAssignmentRepository roomAssignmentRepository;

    @Autowired
    DSLContext ctx;

    private Result<RoomRecord> originalRooms;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalRooms = ctx.selectFrom(ROOM).fetch();
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        originalRooms.forEach(original -> ctx.update(ROOM)
                .set(ROOM.CLEANING_STATUS, original.getCleaningStatus())
                .where(ROOM.ID.eq(original.getId()))
                .execute());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-014", businessRules = {"BR-001"})
    void housekeeper_marks_a_cleaned_room_clean() {
        Result<ReservationRecord> reservationsBefore = ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch();
        logIn("hanna");

        // Step 1: the cleaned room on the cleaning list (UC-013)
        selectRoom("C01");
        // Step 2
        markClean();

        // Step 5: removed from the cleaning list
        assertThat(message()).isEqualTo("Room C01 is clean.");
        assertThat(roomNumbers()).doesNotContain("C01").contains("C02", "C03");
        assertThat($(H2.class).exists()).isFalse();

        // Step 4: cleaning status Clean; BR-001: no reservation is changed
        assertThat(room(2501L).getCleaningStatus()).isEqualTo("CLEAN");
        assertThat(ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch()).isEqualTo(reservationsBefore);
        // Success postcondition: the free, clean room can be assigned at check-in (UC-009)
        assertThat(roomAssignmentRepository.findFreeRooms(2501L, LocalDate.of(2030, 6, 1), LocalDate.of(2030, 6, 2)))
                .filteredOn(RoomCandidate::clean).extracting(RoomCandidate::roomNumber).contains("C01");
    }

    @Test
    @UseCase(id = "UC-014", businessRules = {"BR-002"})
    void occupied_room_can_be_marked_clean_and_stays_occupied() {
        logIn("hanna");

        selectRoom("C04");
        markClean();

        assertThat(room(2504L).getCleaningStatus()).isEqualTo("CLEAN");
        ReservationRecord guest = ctx.selectFrom(RESERVATION)
                .where(RESERVATION.RESERVATION_CODE.eq("CLEAN00003")).fetchSingle();
        assertThat(guest.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(guest.getRoomId()).isEqualTo(2504L);
        assertThat(roomNumbers()).doesNotContain("C04");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Room already clean
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-014", scenario = "A1: Room Already Clean", businessRules = {"BR-001"})
    void housekeeper_is_informed_when_another_housekeeper_marked_the_room_clean_meanwhile() {
        logIn("hanna");
        selectRoom("C03");

        // Another housekeeper marks C03 clean first
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, "CLEAN").where(ROOM.ID.eq(2503L)).execute();
        markClean();

        // A1 step 1: informed, and the cleaning list is refreshed
        assertThat(message()).isEqualTo("Room C03 is already clean.");
        assertThat(roomNumbers()).doesNotContain("C03").contains("C01");
        assertThat(room(2503L).getCleaningStatus()).isEqualTo("CLEAN");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Housekeeper selected the wrong room
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-014", scenario = "A2: Housekeeper Selected the Wrong Room")
    void housekeeper_selects_another_room_before_marking_it_clean() {
        logIn("hanna");
        selectRoom("C01");
        assertThat($(H2.class).single().getText()).isEqualTo("Room C01");

        // A2 step 1: another room; step 2: continues at step 2
        selectRoom("C03");
        assertThat($(H2.class).single().getText()).isEqualTo("Room C03");
        markClean();

        assertThat(room(2503L).getCleaningStatus()).isEqualTo("CLEAN");
        // Failure postcondition for the wrongly selected room: unchanged
        assertThat(room(2501L).getCleaningStatus()).isEqualTo("DIRTY");
        assertThat(roomNumbers()).contains("C01").doesNotContain("C03");
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

    @SuppressWarnings("unchecked")
    private Grid<CleaningListEntry> grid() {
        return $(Grid.class).withId("cleaning-list").single();
    }

    private void selectRoom(String roomNumber) {
        Grid<CleaningListEntry> grid = grid();
        int row = IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getRow(index).roomNumber().equals(roomNumber))
                .findFirst().orElseThrow();
        test(grid).select(row);
    }

    private void markClean() {
        test($(Button.class).withText("Mark clean").single()).click();
    }

    private List<String> roomNumbers() {
        Grid<CleaningListEntry> grid = grid();
        return IntStream.range(0, test(grid).size()).mapToObj(row -> test(grid).getRow(row).roomNumber()).toList();
    }

    private String message() {
        return $(Paragraph.class).withId("cleaning-message").single().getText();
    }

    private RoomRecord room(Long roomId) {
        return ctx.selectFrom(ROOM).where(ROOM.ID.eq(roomId)).fetchSingle();
    }
}
