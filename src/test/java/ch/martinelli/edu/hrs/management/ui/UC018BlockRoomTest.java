package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.management.BlockableRoom;
import ch.martinelli.edu.hrs.management.ReservationsWouldLoseRoomException;
import ch.martinelli.edu.hrs.management.RoomBlockData;
import ch.martinelli.edu.hrs.management.RoomBlockOverview;
import ch.martinelli.edu.hrs.management.RoomBlockService;
import ch.martinelli.edu.hrs.reception.RoomAssignmentRepository;
import ch.martinelli.edu.hrs.reception.RoomCandidate;
import ch.martinelli.edu.hrs.reservation.RoomTypeAvailabilityRepository;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Use case test for UC-018 "Block Room". Test data: V116__uc018_test_data.sql (room type Villa with the active rooms
 * F01 to F03 and the inactive room F04; F01 is blocked from 10 to 12 July 2030; F03 is occupied until the end of
 * 2031); staff members from V103__uc022_test_data.sql (password Secret-123). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC018BlockRoomTest extends HrsBrowserlessTest {

    /** Rows created by the tests get sequence values below the IDs of the test data. */
    private static final long FIRST_TEST_DATA_ID = 1001;
    private static final long VILLA = 2801;
    private static final long F01 = 2801;
    private static final long F02 = 2802;
    private static final LocalDate TODAY = LocalDate.of(2030, 6, 1);

    @TestBean
    Clock clock;

    @Autowired
    RoomBlockService roomBlockService;

    @Autowired
    RoomTypeAvailabilityRepository availabilityRepository;

    @Autowired
    RoomAssignmentRepository roomAssignmentRepository;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @AfterEach
    void removeCreatedBlocks() {
        SecurityContextHolder.clearContext();
        ctx.deleteFrom(ROOM_BLOCK).where(ROOM_BLOCK.ID.lt(FIRST_TEST_DATA_ID)).execute();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", businessRules = {"BR-001", "BR-003"})
    void manager_blocks_a_room() {
        // Before: F02 is free from 20 to 22 June and offered at check-in
        assertThat(freeVillaRooms(LocalDate.of(2030, 6, 20), LocalDate.of(2030, 6, 23))).contains("F02");
        logIn("mark");

        // Step 1
        test($(RouterLink.class).withText("Room blocks").single()).click();
        assertThat(getCurrentView()).isInstanceOf(RoomBlocksView.class);

        // Step 2: the current and future room blocks with room number, dates and reason
        Grid<RoomBlockOverview> grid = grid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Room number", "First blocked date", "Last blocked date", "Reason");
        assertThat(rows("F01")).containsExactly(List.of("F01", "Jul 10, 2030", "Jul 12, 2030", "Painting"));
        // The past block of F02 is not listed
        assertThat(rows("F02")).isEmpty();

        // Step 3: only active rooms can be selected
        test($(Button.class).withText("New room block").single()).click();
        assertThat(roomSelect().getListDataView().getItems().map(BlockableRoom::roomNumber))
                .contains("F01", "F02", "F03").doesNotContain("F04");
        selectRoom("F02");
        test(startDate()).setValue(LocalDate.of(2030, 6, 20));
        test(endDate()).setValue(LocalDate.of(2030, 6, 22));
        test(reason()).setValue("Plumbing");

        // Step 4
        save();

        // Step 7: the updated list
        assertThat(message()).isEqualTo("Room F02 is blocked from Jun 20, 2030 to Jun 22, 2030.");
        assertThat(rows("F02")).containsExactly(List.of("F02", "Jun 20, 2030", "Jun 22, 2030", "Plumbing"));
        assertThat($(DatePicker.class).withCaption("First blocked date").exists()).isFalse();

        // Success postconditions: the block exists, both dates are blocked (BR-001), the availability is reduced
        // and the room is not offered at check-in while blocked
        assertThat(ctx.fetchCount(ROOM_BLOCK, ROOM_BLOCK.ROOM_ID.eq(F02)
                .and(ROOM_BLOCK.START_DATE.eq(LocalDate.of(2030, 6, 20)))
                .and(ROOM_BLOCK.END_DATE.eq(LocalDate.of(2030, 6, 22)))
                .and(ROOM_BLOCK.REASON.eq("Plumbing")))).isEqualTo(1);
        assertThat(availabilityRepository.findBlockedNights(F02, LocalDate.of(2030, 6, 19), LocalDate.of(2030, 6, 24)))
                .containsExactly(LocalDate.of(2030, 6, 20), LocalDate.of(2030, 6, 21), LocalDate.of(2030, 6, 22));
        assertThat(freeVillaRooms(LocalDate.of(2030, 6, 22), LocalDate.of(2030, 6, 23))).doesNotContain("F02");
        assertThat(freeVillaRooms(LocalDate.of(2030, 6, 23), LocalDate.of(2030, 6, 24))).contains("F02");
    }

    @Test
    @UseCase(id = "UC-018", businessRules = {"BR-001"})
    void room_can_be_blocked_from_today_for_one_day_without_a_reason() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F02");
        test(startDate()).setValue(TODAY);
        test(endDate()).setValue(TODAY);

        save();

        assertThat(message()).isEqualTo("Room F02 is blocked from Jun 1, 2030 to Jun 1, 2030.");
        assertThat(rows("F02")).containsExactly(List.of("F02", "Jun 1, 2030", "Jun 1, 2030", ""));
        assertThat(ctx.fetchOne(ROOM_BLOCK, ROOM_BLOCK.ROOM_ID.eq(F02).and(ROOM_BLOCK.START_DATE.eq(TODAY)))
                .getReason()).isNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid dates
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", scenario = "A1: Invalid Dates", businessRules = {"BR-001"})
    void first_blocked_date_in_the_past_is_marked() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F02");
        test(startDate()).setValue(TODAY.minusDays(1));
        test(endDate()).setValue(TODAY.plusDays(2));
        test(reason()).setValue("Plumbing");

        save();

        assertThat(startDate().isInvalid()).isTrue();
        assertThat(createdBlocks()).isZero();

        // A1 step 2: the entered values are kept and the use case continues at step 3
        assertThat(roomSelect().getValue().roomNumber()).isEqualTo("F02");
        assertThat(endDate().getValue()).isEqualTo(TODAY.plusDays(2));
        assertThat(reason().getValue()).isEqualTo("Plumbing");
        test(startDate()).setValue(TODAY.plusDays(1));
        save();
        assertThat(message()).isEqualTo("Room F02 is blocked from Jun 2, 2030 to Jun 3, 2030.");
    }

    @Test
    @UseCase(id = "UC-018", scenario = "A1: Invalid Dates", businessRules = {"BR-001"})
    void last_blocked_date_before_the_first_blocked_date_is_marked() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F02");
        test(startDate()).setValue(LocalDate.of(2030, 6, 22));
        test(endDate()).setValue(LocalDate.of(2030, 6, 21));

        save();

        assertThat(endDate().isInvalid()).isTrue();
        assertThat(startDate().isInvalid()).isFalse();
        assertThat(createdBlocks()).isZero();
    }

    @Test
    @UseCase(id = "UC-018", scenario = "A1: Invalid Dates")
    void missing_values_are_marked() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();

        save();

        assertThat(roomSelect().isInvalid()).isTrue();
        assertThat(startDate().isInvalid()).isTrue();
        assertThat(endDate().isInvalid()).isTrue();
        assertThat(createdBlocks()).isZero();
    }

    @Test
    @UseCase(id = "UC-018", scenario = "A1: Invalid Dates")
    void reason_longer_than_200_characters_is_refused() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();

        // The reason cannot get longer than 200 characters
        assertThat(reason().getMaxLength()).isEqualTo(200);
        assertThatThrownBy(() -> roomBlockService.block(new RoomBlockData(F02, LocalDate.of(2030, 6, 20),
                LocalDate.of(2030, 6, 20), "X".repeat(201))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(createdBlocks()).isZero();

        selectRoom("F02");
        test(startDate()).setValue(LocalDate.of(2030, 6, 20));
        test(endDate()).setValue(LocalDate.of(2030, 6, 20));
        test(reason()).setValue("X".repeat(200));
        save();
        assertThat(message()).isEqualTo("Room F02 is blocked from Jun 20, 2030 to Jun 20, 2030.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: Overlapping block
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", scenario = "A2: Overlapping Block", businessRules = {"BR-001"})
    void block_overlapping_another_block_of_the_room_is_refused() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F01");
        // The last day of the existing block is blocked too
        test(startDate()).setValue(LocalDate.of(2030, 7, 12));
        test(endDate()).setValue(LocalDate.of(2030, 7, 15));

        save();

        assertThat(message()).isEqualTo("Room F01 is already blocked from Jul 10, 2030 to Jul 12, 2030.");
        assertThat(createdBlocks()).isZero();
        assertThat(rows("F01")).containsExactly(List.of("F01", "Jul 10, 2030", "Jul 12, 2030", "Painting"));

        // A2 step 2: continues at step 3; the block right after the existing one is accepted
        test(startDate()).setValue(LocalDate.of(2030, 7, 13));
        save();
        assertThat(message()).isEqualTo("Room F01 is blocked from Jul 13, 2030 to Jul 15, 2030.");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Room occupied during the block
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", scenario = "A3: Room Occupied During the Block", businessRules = {"BR-002"})
    void occupied_room_cannot_be_blocked() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F03");
        test(startDate()).setValue(LocalDate.of(2030, 8, 1));
        test(endDate()).setValue(LocalDate.of(2030, 8, 3));

        save();

        assertThat(message()).isEqualTo("Room F03 is occupied by reservation VILLA00001 during the block.");
        assertThat(createdBlocks()).isZero();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A4: Reservations would lose their room
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", scenario = "A4: Reservations Would Lose Their Room", businessRules = {"BR-003"})
    void block_is_refused_when_reservations_would_lose_their_room() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F02");
        // 14 June has one reservation; 15 and 16 June have three reservations for three rooms
        test(startDate()).setValue(LocalDate.of(2030, 6, 14));
        test(endDate()).setValue(LocalDate.of(2030, 6, 16));

        save();

        assertThat(message()).isEqualTo("The Villa would have more reservations than rooms on these nights: "
                + "Jun 15, 2030, Jun 16, 2030.");
        // Failure postconditions: no block is recorded and the availability is unchanged
        assertThat(createdBlocks()).isZero();
        assertThat(availabilityRepository.findUnavailableNights(VILLA, LocalDate.of(2030, 6, 14),
                LocalDate.of(2030, 6, 17), -1L))
                .containsExactly(LocalDate.of(2030, 6, 15), LocalDate.of(2030, 6, 16));

        // A4 step 2: continues at step 3; the night after the reservations can be blocked
        test(startDate()).setValue(LocalDate.of(2030, 6, 17));
        test(endDate()).setValue(LocalDate.of(2030, 6, 17));
        save();
        assertThat(message()).isEqualTo("Room F02 is blocked from Jun 17, 2030 to Jun 17, 2030.");
    }

    @Test
    @UseCase(id = "UC-018", scenario = "A4: Reservations Would Lose Their Room", businessRules = {"BR-003"})
    void concurrent_blocks_never_leave_a_reservation_without_a_room() throws Exception {
        // 25 June: two reservations (one Cancelled reservation does not count) for three rooms, so one room is left
        LocalDate night = LocalDate.of(2030, 6, 25);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> blocks = List.of(F01, F02).stream()
                .<Callable<Boolean>>map(roomId -> () -> {
                    start.await();
                    try {
                        roomBlockService.block(new RoomBlockData(roomId, night, night, "Concurrent"));
                        return true;
                    } catch (ReservationsWouldLoseRoomException _) {
                        return false;
                    }
                })
                .toList();

        List<Boolean> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = blocks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
        }

        // NFR-004: exactly one block succeeds, so the room type is never overbooked
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(createdBlocks()).isEqualTo(1);
        assertThat(availabilityRepository.findUnavailableNights(VILLA, night, night.plusDays(1), -1L))
                .containsExactly(night);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A5: Manager discards the block
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018", scenario = "A5: Manager Discards the Block")
    void entered_values_are_discarded_when_the_manager_leaves() {
        openRoomBlocks();
        test($(Button.class).withText("New room block").single()).click();
        selectRoom("F02");
        test(startDate()).setValue(LocalDate.of(2030, 6, 20));
        test(endDate()).setValue(LocalDate.of(2030, 6, 22));

        test($(Button.class).withText("Discard").single()).click();

        assertThat(createdBlocks()).isZero();
        assertThat(rows("F02")).isEmpty();
        assertThat($(DatePicker.class).withCaption("First blocked date").exists()).isFalse();

        // A new block starts empty
        test($(Button.class).withText("New room block").single()).click();
        assertThat(roomSelect().getValue()).isNull();
        assertThat(startDate().getValue()).isNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the manager is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-018")
    void only_a_logged_in_manager_can_block_rooms() {
        navigate("room-blocks", LoginView.class);

        logIn("rita");
        assertThat($(RouterLink.class).withText("Room blocks").exists()).isFalse();
        assertThat(navigate("room-blocks", RouteNotFoundError.class)).isNotNull();
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

    private void openRoomBlocks() {
        logIn("mark");
        navigate(RoomBlocksView.class);
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomBlockOverview> grid() {
        return $(Grid.class).withId("room-blocks").single();
    }

    private List<List<String>> rows(String roomNumber) {
        Grid<RoomBlockOverview> grid = grid();
        return IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getRow(index).roomNumber().equals(roomNumber))
                .mapToObj(index -> IntStream.range(0, grid.getColumns().size())
                        .mapToObj(column -> test(grid).getCellText(index, column)).toList())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Select<BlockableRoom> roomSelect() {
        return $(Select.class).withCaption("Room").single();
    }

    private void selectRoom(String roomNumber) {
        test(roomSelect()).selectItem(roomNumber + " (Villa)");
    }

    private DatePicker startDate() {
        return $(DatePicker.class).withCaption("First blocked date").single();
    }

    private DatePicker endDate() {
        return $(DatePicker.class).withCaption("Last blocked date").single();
    }

    private TextArea reason() {
        return $(TextArea.class).withCaption("Reason").single();
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private String message() {
        return $(Paragraph.class).withId("room-block-message").single().getText();
    }

    private int createdBlocks() {
        return ctx.fetchCount(ROOM_BLOCK, ROOM_BLOCK.ID.lt(FIRST_TEST_DATA_ID));
    }

    private List<String> freeVillaRooms(LocalDate arrival, LocalDate departure) {
        return roomAssignmentRepository.findFreeRooms(VILLA, arrival, departure).stream()
                .map(RoomCandidate::roomNumber).toList();
    }
}
