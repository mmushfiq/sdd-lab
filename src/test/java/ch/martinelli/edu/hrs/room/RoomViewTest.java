package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "MANAGER")
class RoomViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @BeforeEach
    void createRoomType() {
        roomTypeService.save(roomType("Test Double", 2, "120.00"));
    }

    @Test
    void createRoomWithTypeIsClean() {
        navigate(RoomView.class);

        test($(TextField.class).withLabel("Room number").single()).setValue("T401");
        test($(ComboBox.class).withLabel("Room type").single()).selectItem("Test Double");
        test($(Button.class).withText("Save").single()).click();

        var grid = test(grid());
        var row = IntStream.range(0, grid.size())
                .filter(i -> grid.getRow(i).getRoomNumber().equals("T401"))
                .findFirst().orElseThrow();
        assertThat(grid.getCellText(row, 1)).isEqualTo("Test Double");
        assertThat(grid.getCellText(row, 2)).isEqualTo("Clean");
    }

    @Test
    void twoRoomsInARowAreBothCreated() {
        navigate(RoomView.class);

        for (var number : new String[]{"T411", "T412"}) {
            test($(TextField.class).withLabel("Room number").single()).setValue(number);
            test($(ComboBox.class).withLabel("Room type").single()).selectItem("Test Double");
            test($(Button.class).withText("Save").single()).click();
        }

        assertThat(roomService.findAll()).extracting(RoomRecord::getRoomNumber).contains("T411", "T412");
        assertThat($(TextField.class).withLabel("Room number").single().getValue()).isEmpty();
    }

    @Test
    void duplicateRoomNumberShowsError() {
        var existing = new RoomRecord();
        existing.setRoomNumber("T402");
        existing.setRoomTypeId(roomTypeService.findAll().getFirst().getId());
        roomService.save(existing);
        navigate(RoomView.class);

        test($(TextField.class).withLabel("Room number").single()).setValue("T402");
        test($(ComboBox.class).withLabel("Room type").single()).selectItem("Test Double");
        test($(Button.class).withText("Save").single()).click();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("Room number is already used.");
    }

    @Test
    void deleteRoom() {
        var room = new RoomRecord();
        room.setRoomNumber("T403");
        room.setRoomTypeId(roomTypeService.findAll().getFirst().getId());
        roomService.save(room);
        navigate(RoomView.class);

        var grid = test(grid());
        grid.select(IntStream.range(0, grid.size())
                .filter(i -> grid.getRow(i).getRoomNumber().equals("T403"))
                .findFirst().orElseThrow());
        test($(Button.class).withText("Delete").single()).click();

        assertThat(IntStream.range(0, grid.size()).mapToObj(i -> grid.getRow(i).getRoomNumber()))
                .doesNotContain("T403");
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(RoomView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(RoomView.class)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomRecord> grid() {
        return $(Grid.class).single();
    }
}
