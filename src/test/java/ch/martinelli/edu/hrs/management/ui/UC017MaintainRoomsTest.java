package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.management.RoomData;
import ch.martinelli.edu.hrs.management.RoomOverview;
import ch.martinelli.edu.hrs.management.RoomService;
import ch.martinelli.edu.hrs.management.RoomTypeOverview;
import ch.martinelli.edu.hrs.reservation.RoomTypeAvailabilityRepository;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
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
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-017 "Maintain Rooms". Test data: V115__uc017_test_data.sql (room type Bungalow with rooms E01
 * to E03), the Garden Room from V114__uc016_test_data.sql and the rooms of V100__uc001_test_data.sql; staff members
 * from V103__uc022_test_data.sql (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC017MaintainRoomsTest extends HrsBrowserlessTest {

    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;
    private static final long BUNGALOW = 2701;

    @TestBean
    Clock clock;

    @Autowired
    RoomService roomService;

    @Autowired
    RoomTypeAvailabilityRepository availabilityRepository;

    @Autowired
    DSLContext ctx;

    private Result<RoomRecord> originalRooms;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalRooms = ctx.selectFrom(ROOM).where(ROOM.ID.ge(FIRST_TEST_DATA_ID)).fetch();
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        ctx.deleteFrom(ROOM).where(ROOM.ID.lt(FIRST_TEST_DATA_ID)).execute();
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
    @UseCase(id = "UC-017", businessRules = {"BR-001", "BR-002"})
    void manager_creates_a_new_room() {
        // Before: on the night of 10 June the Bungalow has three rooms and three reservations, so it is full
        assertThat(unavailableBungalowNights(LocalDate.of(2030, 6, 10))).containsExactly(LocalDate.of(2030, 6, 10));
        logIn("mark");

        // Step 1
        test($(RouterLink.class).withText("Rooms").single()).click();
        assertThat(getCurrentView()).isInstanceOf(RoomsView.class);

        // Step 2: room number, room type, cleaning status and whether active, ordered by room number
        Grid<RoomOverview> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Room number", "Room type", "Cleaning status", "Status");
        assertThat(row("101")).containsExactly("101", "Single Room", "Clean", "Active");
        assertThat(row("203")).containsExactly("203", "Double Room", "Clean", "Inactive");
        assertThat(roomNumbers()).isSorted();

        // Steps 3 and 4
        test($(Button.class).withText("New room").single()).click();
        test(roomNumberField()).setValue("E04");
        test(roomTypeSelect()).selectItem("Bungalow");
        // Step 5
        save();

        // Step 8: the updated list; BR-002: a new room starts clean
        assertThat(message()).isEqualTo("Room E04 is saved.");
        assertThat(row("E04")).containsExactly("E04", "Bungalow", "Clean", "Active");
        RoomRecord saved = ctx.selectFrom(ROOM).where(ROOM.ROOM_NUMBER.eq("E04")).fetchSingle();
        assertThat(saved.getRoomTypeId()).isEqualTo(BUNGALOW);
        assertThat(saved.getCleaningStatus()).isEqualTo("CLEAN");
        assertThat(saved.getActive()).isTrue();
        // Availability is calculated with the saved room
        assertThat(unavailableBungalowNights(LocalDate.of(2030, 6, 10))).isEmpty();
    }

    @Test
    @UseCase(id = "UC-017", businessRules = {"BR-003"})
    void room_can_be_deactivated_when_every_reservation_keeps_a_room() {
        // The Garden Room has two active rooms and at most one reservation per night
        openRooms();
        select("D02");

        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        assertThat(message()).isEqualTo("Room D02 is saved.");
        assertThat(row("D02")).containsExactly("D02", "Garden Room", "Clean", "Inactive");
        // BR-004: rooms are never deleted
        assertThat(ctx.fetchExists(ROOM, ROOM.ROOM_NUMBER.eq("D02"))).isTrue();
        assertThat($(Button.class).withText("Delete").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-017")
    void manager_activates_an_inactive_room_and_changes_its_room_number() {
        openRooms();
        select("D03");

        test(roomNumberField()).setValue("D04");
        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        RoomRecord room = ctx.selectFrom(ROOM).where(ROOM.ID.eq(2603L)).fetchSingle();
        assertThat(room.getRoomNumber()).isEqualTo("D04");
        assertThat(room.getActive()).isTrue();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid values
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017", scenario = "A1: Invalid Values")
    void missing_room_number_and_room_type_are_marked() {
        openRooms();
        test($(Button.class).withText("New room").single()).click();
        test(roomNumberField()).setValue("   ");

        save();

        assertThat(roomNumberField().isInvalid()).isTrue();
        assertThat(roomTypeSelect().isInvalid()).isTrue();
        assertThat(ctx.fetchCount(ROOM, ROOM.ID.lt(FIRST_TEST_DATA_ID))).isZero();

        // The room number cannot get longer than 10 characters
        assertThat(roomNumberField().getMaxLength()).isEqualTo(10);
        assertThatThrownBy(() -> roomService.save(null, new RoomData("X".repeat(11), BUNGALOW, true)))
                .isInstanceOf(IllegalArgumentException.class);

        // A1 step 2: continues at step 4; the entered values are kept
        test(roomTypeSelect()).selectItem("Bungalow");
        assertThat(roomNumberField().getValue()).isEqualTo("   ");
        test(roomNumberField()).setValue("X".repeat(10));
        save();
        assertThat(message()).isEqualTo("Room XXXXXXXXXX is saved.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Room number already used
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017", scenario = "A2: Room Number Already Used", businessRules = {"BR-001"})
    void manager_is_informed_when_the_room_number_is_already_used() {
        openRooms();
        test($(Button.class).withText("New room").single()).click();
        test(roomNumberField()).setValue("101");
        test(roomTypeSelect()).selectItem("Bungalow");

        save();

        assertThat(message()).isEqualTo(RoomsView.NUMBER_USED);
        assertThat(roomNumberField().isInvalid()).isTrue();
        assertThat(ctx.fetchCount(ROOM, ROOM.ID.lt(FIRST_TEST_DATA_ID))).isZero();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Reservations would lose their room
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017", scenario = "A3: Reservations Would Lose Their Room", businessRules = {"BR-003"})
    void deactivation_is_refused_when_reservations_would_lose_their_room() {
        RoomRecord before = room("E01");
        openRooms();
        select("E01");

        test($(Checkbox.class).withCaption("Active").single()).click();
        save();

        // 10 June: three reservations; 20 June: two reservations while E02 is blocked
        assertThat(message()).isEqualTo("The Bungalow would have more reservations than rooms on these nights: "
                + "Jun 10, 2030, Jun 20, 2030.");
        // Failure postcondition: the room keeps its previous data
        assertThat(room("E01")).isEqualTo(before);
    }

    @Test
    @UseCase(id = "UC-017", scenario = "A3: Reservations Would Lose Their Room", businessRules = {"BR-003"})
    void moving_a_room_to_another_room_type_is_refused_when_reservations_would_lose_their_room() {
        RoomRecord before = room("E01");
        openRooms();
        select("E01");

        test(roomTypeSelect()).selectItem("Garden Room");
        save();

        assertThat(message()).startsWith("The Bungalow would have more reservations than rooms on these nights:");
        assertThat(room("E01")).isEqualTo(before);

        // A3 step 2: continues at step 4
        test(roomTypeSelect()).selectItem("Bungalow");
        test(roomNumberField()).setValue("E10");
        save();
        assertThat(room("E10").getRoomTypeId()).isEqualTo(BUNGALOW);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Room is occupied
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017", scenario = "A4: Room Is Occupied")
    void occupied_room_cannot_be_deactivated_or_moved() {
        RoomRecord before = room("E03");
        openRooms();
        select("E03");

        test($(Checkbox.class).withCaption("Active").single()).click();
        save();
        assertThat(message()).isEqualTo(RoomsView.OCCUPIED);

        test($(Checkbox.class).withCaption("Active").single()).click();
        test(roomTypeSelect()).selectItem("Garden Room");
        save();
        assertThat(message()).isEqualTo(RoomsView.OCCUPIED);
        assertThat(room("E03")).isEqualTo(before);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Manager discards the changes
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017", scenario = "A5: Manager Discards the Changes")
    void entered_values_are_discarded_when_the_manager_leaves_the_room() {
        RoomRecord before = room("E01");
        openRooms();
        select("E01");
        test(roomNumberField()).setValue("E99");

        test($(Button.class).withText("Discard").single()).click();

        assertThat(room("E01")).isEqualTo(before);
        assertThat(roomNumbers()).contains("E01").doesNotContain("E99");
        assertThat($(TextField.class).withCaption("Room number").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the manager is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-017")
    void only_a_logged_in_manager_can_maintain_rooms() {
        navigate("rooms", LoginView.class);

        logIn("hanna");
        assertThat(navigate("rooms", RouteNotFoundError.class)).isNotNull();
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

    private void openRooms() {
        logIn("mark");
        navigate(RoomsView.class);
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomOverview> grid() {
        return $(Grid.class).withId("rooms").single();
    }

    private int rowIndex(String roomNumber) {
        Grid<RoomOverview> grid = grid();
        return IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getRow(index).roomNumber().equals(roomNumber))
                .findFirst().orElseThrow(() -> new AssertionError(roomNumber + " is not listed"));
    }

    private List<String> row(String roomNumber) {
        Grid<RoomOverview> grid = grid();
        int index = rowIndex(roomNumber);
        return IntStream.range(0, grid.getColumns().size()).mapToObj(column -> test(grid).getCellText(index, column))
                .toList();
    }

    private List<String> roomNumbers() {
        Grid<RoomOverview> grid = grid();
        return IntStream.range(0, test(grid).size()).mapToObj(row -> test(grid).getRow(row).roomNumber()).toList();
    }

    private void select(String roomNumber) {
        test(grid()).select(rowIndex(roomNumber));
    }

    private TextField roomNumberField() {
        return $(TextField.class).withCaption("Room number").single();
    }

    @SuppressWarnings("unchecked")
    private Select<RoomTypeOverview> roomTypeSelect() {
        return $(Select.class).withCaption("Room type").single();
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private String message() {
        return $(Paragraph.class).withId("room-message").single().getText();
    }

    private List<LocalDate> unavailableBungalowNights(LocalDate night) {
        return availabilityRepository.findUnavailableNights(BUNGALOW, night, night.plusDays(1), -1L);
    }

    private RoomRecord room(String roomNumber) {
        return ctx.selectFrom(ROOM).where(ROOM.ROOM_NUMBER.eq(roomNumber)).fetchSingle();
    }
}
