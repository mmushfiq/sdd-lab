package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reception.Occupancy;
import ch.martinelli.edu.hrs.reservation.RoomTypeChoice;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-011 "View Room Status Board". Test data: V111__uc011_test_data.sql (room type Tent with rooms
 * A01 to A03) and the rooms of the other test data, e.g. the Cabin rooms 701 to 706 (V109); staff members from
 * V103__uc022_test_data.sql (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC011ViewRoomStatusBoardTest extends HrsBrowserlessTest {

    @TestBean
    Clock clock;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @AfterEach
    void restoreTestData() {
        SecurityContextHolder.clearContext();
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, "CLEAN").where(ROOM.ID.eq(2101L)).execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-011", businessRules = {"BR-001", "BR-002"})
    void receptionist_sees_every_active_room_with_occupancy_and_cleaning_status() {
        Result<Record> dataBefore = allRoomAndReservationData();
        logIn("rita");

        // Step 1
        test($(RouterLink.class).withText("Room status").single()).click();
        assertThat(getCurrentView()).isInstanceOf(RoomStatusBoardView.class);

        // Step 2: every active room, ordered by room number; inactive rooms 203, 705 and A03 are not shown
        List<String> roomNumbers = roomNumbers();
        assertThat(roomNumbers).hasSize(ctx.fetchCount(ROOM, ROOM.ACTIVE.isTrue()));
        assertThat(roomNumbers).isSorted().doesNotContain("203", "705", "A03")
                .contains("101", "701", "703", "706", "A01", "A02");

        // Free and Clean: highlighted as assignable
        assertThat(texts("701")).containsExactly("701", "Cabin", "Free", "Clean", "Assignable");
        assertThat(tile("701").getClassNames()).contains("assignable");
        // Free but dirty: not assignable
        assertThat(texts("703")).containsExactly("703", "Cabin", "Free", "Dirty");
        assertThat(tile("703").getClassNames()).doesNotContain("assignable");

        // Step 3: guest and departure date of an occupied room
        assertThat(texts("706")).containsExactly("706", "Cabin", "Occupied", "Clean", "Konrad Konkurrent",
                "Departs Dec 31, 2031");
        // Last blocked date and reason of a blocked room
        assertThat(texts("A01")).containsExactly("A01", "Tent", "Blocked", "Clean", "Blocked until Jun 3, 2030",
                "Renovation");
        // BR-001: occupied and also blocked today
        assertThat(texts("A02")).containsExactly("A02", "Tent", "Occupied (blocked today)", "Dirty", "Lena Lager",
                "Departs Dec 31, 2031");

        // Success postcondition: no room or reservation data is changed
        assertThat(allRoomAndReservationData()).isEqualTo(dataBefore);
    }

    @Test
    @UseCase(id = "UC-011", businessRules = {"BR-001"})
    void room_with_a_block_that_does_not_cover_today_is_free() {
        openBoard();

        // Room 704 is blocked for the night of 2 June only
        assertThat(texts("704")).containsExactly("704", "Cabin", "Free", "Clean", "Assignable");
    }

    @Test
    @UseCase(id = "UC-011", businessRules = {"BR-003"})
    void board_wraps_the_rooms_instead_of_scrolling_horizontally() {
        openBoard();

        Div board = $(Div.class).withId("board").single();
        assertThat(board.getStyle().get("display")).isEqualTo("grid");
        assertThat(board.getStyle().get("grid-template-columns")).isEqualTo("repeat(auto-fill, minmax(10rem, 1fr))");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Receptionist filters the board
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-011", scenario = "A1: Receptionist Filters the Board")
    void receptionist_filters_by_room_type_occupancy_and_cleaning_status() {
        openBoard();

        // A1 step 1 and 2: room type
        test(roomTypeFilter()).selectItem("Cabin");
        assertThat(roomNumbers()).containsExactly("701", "702", "703", "704", "706");

        // Room type and occupancy
        test(occupancyFilter()).selectItem("Free");
        assertThat(roomNumbers()).containsExactly("701", "702", "703", "704");

        // Room type, occupancy and cleaning status: the assignable Cabins
        test(cleaningFilter()).selectItem("Clean");
        assertThat(roomNumbers()).containsExactly("701", "702", "704");
        // A1 step 3: continues at step 3
        assertThat(texts("702")).contains("Assignable");
    }

    @Test
    @UseCase(id = "UC-011", scenario = "A1: Receptionist Filters the Board")
    void filter_by_occupancy_alone_shows_only_matching_rooms() {
        openBoard();

        test(occupancyFilter()).selectItem("Blocked");

        assertThat(roomNumbers()).contains("A01").doesNotContain("A02", "701", "706");
        assertThat($(Span.class).withClassName("occupancy").all()).extracting(Span::getText).containsOnly("Blocked");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Status changed while viewing
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-011", scenario = "A2: Status Changed While Viewing")
    void refreshed_board_shows_the_changed_status() {
        openBoard();
        assertThat(texts("701")).contains("Clean");

        // Meanwhile room 701 is set to dirty
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ID.eq(2101L)).execute();
        assertThat(texts("701")).contains("Clean");

        // A2 step 1: the receptionist refreshes the board; step 2: displayed again
        test($(Button.class).withText("Refresh").single()).click();

        assertThat(texts("701")).containsExactly("701", "Cabin", "Free", "Dirty");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-011")
    void only_a_logged_in_receptionist_can_view_the_room_status_board() {
        navigate("room-status-board", LoginView.class);

        logIn("hanna");
        assertThat($(RouterLink.class).withText("Room status").exists()).isFalse();
        assertThat(navigate("room-status-board", RouteNotFoundError.class)).isNotNull();
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

    private void openBoard() {
        logIn("rita");
        navigate(RoomStatusBoardView.class);
    }

    private List<String> roomNumbers() {
        return $(H3.class, board()).all().stream().map(H3::getText).toList();
    }

    private Div board() {
        return $(Div.class).withId("board").single();
    }

    private Div tile(String roomNumber) {
        return $(Div.class).withId("room-" + roomNumber).single();
    }

    /**
     * The texts of a room tile: the room number and every line.
     */
    private List<String> texts(String roomNumber) {
        Div tile = tile(roomNumber);
        return tile.getChildren()
                .map(child -> child instanceof H3 h3 ? h3.getText()
                        : child instanceof Span span ? span.getText() : null)
                .filter(text -> text != null && !text.isEmpty())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Select<RoomTypeChoice> roomTypeFilter() {
        return $(Select.class).withCaption("Room type").single();
    }

    @SuppressWarnings("unchecked")
    private Select<Occupancy> occupancyFilter() {
        return $(Select.class).withCaption("Occupancy").single();
    }

    @SuppressWarnings("unchecked")
    private Select<Boolean> cleaningFilter() {
        return $(Select.class).withCaption("Cleaning status").single();
    }

    private Result<Record> allRoomAndReservationData() {
        return ctx.select().from(ROOM).leftJoin(RESERVATION).on(RESERVATION.ROOM_ID.eq(ROOM.ID))
                .orderBy(ROOM.ID, RESERVATION.ID).fetch();
    }
}
