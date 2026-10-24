package ch.martinelli.edu.hrs.housekeeping.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.housekeeping.CleaningListEntry;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-013 "View Cleaning List". Test data: V113__uc013_test_data.sql (room type Pod with rooms C01 to
 * C08) and the dirty rooms of other test data (602, 703, 801, 902, A02); staff members from V103__uc022_test_data.sql
 * (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC013ViewCleaningListTest extends HrsBrowserlessTest {

    @TestBean
    Clock clock;

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
    @UseCase(id = "UC-013", businessRules = {"BR-001", "BR-002"})
    void housekeeper_sees_the_dirty_rooms_with_todays_departures_first() {
        Result<RoomRecord> roomsBefore = ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch();

        // Step 1: the cleaning list is the housekeeper's start screen
        logIn("hanna");
        assertThat(getCurrentView()).isInstanceOf(CleaningListView.class);

        // Step 2: room number, room type, occupancy and today's departure
        Grid<CleaningListEntry> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Room", "Room type", "Occupancy", "Departure");
        List<List<String>> rows = rows(grid);
        // BR-001: checked out today, departs today, other free rooms, other occupied rooms; by room number in each
        // group. BR-002: C05 is blocked today; C06 is clean and C07 inactive
        assertThat(rows).filteredOn(row -> row.getFirst().startsWith("C")).containsExactly(
                List.of("C01", "Pod", "Free", "Checked out today"),
                List.of("C02", "Pod", "Occupied", "Departs today"),
                List.of("C03", "Pod", "Free", ""),
                List.of("C08", "Pod", "Free", ""),
                List.of("C04", "Pod", "Occupied", ""));
        assertThat(rows).extracting(List::getFirst).doesNotContain("C05", "C06", "C07", "A02");

        // The whole list follows the cleaning order, also with the dirty rooms of other test data
        List<CleaningListEntry> entries = IntStream.range(0, test(grid).size()).mapToObj(test(grid)::getRow).toList();
        assertThat(entries).isSortedAccordingTo(Comparator.comparingInt(CleaningListEntry::cleaningGroup)
                .thenComparing(CleaningListEntry::roomNumber));
        assertThat(rows).contains(List.of("703", "Cabin", "Free", ""), List.of("902", "Chalet", "Occupied", ""));

        // Success postcondition: no room data is changed
        assertThat(ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch()).isEqualTo(roomsBefore);
    }

    @Test
    @UseCase(id = "UC-013")
    void selected_room_is_displayed_with_the_option_to_mark_it_clean() {
        logIn("hanna");

        // Step 3
        test(grid()).select(0);

        // Step 4
        assertThat($(H2.class).withText("Room C01").exists()).isTrue();
        assertThat($(Paragraph.class).withId("room-details").single().getText())
                .isEqualTo("Pod, Free, Checked out today");
        assertThat($(Button.class).withText("Mark clean").exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-013", businessRules = {"BR-003"})
    void cleaning_list_fits_a_tablet_without_horizontal_scrolling() {
        logIn("hanna");

        Grid<CleaningListEntry> grid = grid();
        assertThat(grid.getColumns()).hasSize(4);
        assertThat(grid.getThemeNames()).contains(GridVariant.LUMO_WRAP_CELL_CONTENT.getVariantName());
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: No dirty rooms
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-013", scenario = "A1: No Dirty Rooms", businessRules = {"BR-002"})
    void housekeeper_is_informed_when_all_rooms_are_clean() {
        // Every room that is not blocked today has been cleaned; A02 and C05 are blocked and stay dirty
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, "CLEAN").where(ROOM.ID.notIn(2302L, 2505L)).execute();

        logIn("hanna");

        assertThat($(Paragraph.class).withId("all-clean").single().getText()).isEqualTo(CleaningListView.ALL_CLEAN);
        assertThat($(Grid.class).exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Housekeeper only reviews the list
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-013", scenario = "A2: Housekeeper Only Reviews the List")
    void housekeeper_leaves_the_list_without_selecting_a_room() {
        Result<RoomRecord> roomsBefore = ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch();
        logIn("hanna");

        test($(Button.class).withText("Log out").single()).click();

        assertThat(ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch()).isEqualTo(roomsBefore);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the housekeeper is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-013")
    void only_a_logged_in_housekeeper_can_view_the_cleaning_list() {
        navigate("cleaning-list", LoginView.class);

        logIn("rita");
        assertThat(navigate("cleaning-list", RouteNotFoundError.class)).isNotNull();
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

    private List<List<String>> rows(Grid<?> grid) {
        int columns = grid.getColumns().size();
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> IntStream.range(0, columns).mapToObj(column -> test(grid).getCellText(row, column))
                        .toList())
                .toList();
    }
}
