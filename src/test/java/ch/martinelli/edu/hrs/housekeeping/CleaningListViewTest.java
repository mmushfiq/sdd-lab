package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.ReservationView;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import ch.martinelli.edu.hrs.room.RoomView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "HOUSEKEEPING")
class CleaningListViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void createRooms() {
        var type = roomTypeService.save(roomType("Test Housekeeping", 2, "100.00"));
        for (var number : new String[]{"T731", "T732"}) {
            var room = new RoomRecord();
            room.setRoomNumber(number);
            room.setRoomTypeId(type.getId());
            roomService.save(room);
        }
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ROOM_NUMBER.eq("T731")).execute();
    }

    @Test
    void listsDirtyRoomsWithRoomNumberAndStatusOnly() {
        navigate(CleaningListView.class);

        assertThat(rowOf("T731")).isNotNegative();
        assertThat(rowOf("T732")).isNegative();
        assertThat(grid().getColumns()).extracting(Grid.Column::getHeaderText).containsExactly("Room", "Status");
        assertThat(test(grid()).getCellText(rowOf("T731"), 1)).isEqualTo("Dirty");
    }

    @Test
    void markCleanedRemovesTheRoom() {
        navigate(CleaningListView.class);

        test(grid()).select(rowOf("T731"));
        test($(Button.class).withText("Mark cleaned").single()).click();

        assertThat(rowOf("T731")).isNegative();
        assertThat(dsl.fetchSingle(ROOM, ROOM.ROOM_NUMBER.eq("T731")).getCleaningStatus()).isEqualTo("CLEAN");
    }

    @Test
    void refreshShowsRoomsCheckedOutMeanwhile() {
        navigate(CleaningListView.class);
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ROOM_NUMBER.eq("T732")).execute();

        test($(Button.class).withText("Refresh").single()).click();

        assertThat(rowOf("T732")).isNotNegative();
        assertThat(UI.getCurrent().getPollInterval()).isEqualTo(CleaningListView.POLL_MILLIS);
    }

    @Test
    void housekeepingIsDeniedOnReservationsAndRooms() {
        assertThat(opens(ReservationView.class)).isFalse();
        assertThat(opens(RoomView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(CleaningListView.class)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomRecord> grid() {
        return $(Grid.class).single();
    }

    private int rowOf(String roomNumber) {
        var grid = test(grid());
        return IntStream.range(0, grid.size()).filter(i -> grid.getRow(i).getRoomNumber().equals(roomNumber))
                .findFirst().orElse(-1);
    }
}
