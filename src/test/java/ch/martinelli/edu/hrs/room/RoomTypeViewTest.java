package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "MANAGER")
class RoomTypeViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Test
    void createRoomType() {
        navigate(RoomTypeView.class);

        test($(TextField.class).withLabel("Name").single()).setValue("Test Family");
        test($(IntegerField.class).withLabel("Capacity").single()).setValue(4);
        test($(BigDecimalField.class).withLabel("Nightly rate").single()).setValue(new BigDecimal("180.00"));
        test($(Button.class).withText("Save").single()).click();

        assertThat(gridNames()).contains("Test Family");
    }

    @Test
    void changeNightlyRate() {
        var type = roomTypeService.save(roomType("Test Double", 2, "120.00"));
        navigate(RoomTypeView.class);

        selectRow(type.getId());
        test($(BigDecimalField.class).withLabel("Nightly rate").single()).setValue(new BigDecimal("140.00"));
        test($(Button.class).withText("Save").single()).click();

        assertThat(roomTypeService.findAll())
                .filteredOn(t -> t.getId().equals(type.getId()))
                .singleElement()
                .satisfies(t -> assertThat(t.getNightlyRate()).isEqualByComparingTo("140.00"));
    }

    @Test
    void deletingTypeInUseShowsError() {
        var type = roomTypeService.save(roomType("Test Suite", 4, "250.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T301");
        room.setRoomTypeId(type.getId());
        roomService.save(room);
        navigate(RoomTypeView.class);

        selectRow(type.getId());
        test($(Button.class).withText("Delete").single()).click();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("Room type is still in use.");
        assertThat(gridNames()).contains("Test Suite");
    }

    @Test
    void deleteUnusedRoomType() {
        var type = roomTypeService.save(roomType("Test Twin", 2, "110.00"));
        navigate(RoomTypeView.class);

        selectRow(type.getId());
        test($(Button.class).withText("Delete").single()).click();

        assertThat(gridNames()).doesNotContain("Test Twin");
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void receptionistIsDenied() {
        assertThat(opens(RoomTypeView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(RoomTypeView.class)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomTypeRecord> grid() {
        return $(Grid.class).single();
    }

    private java.util.List<String> gridNames() {
        var grid = test(grid());
        return java.util.stream.IntStream.range(0, grid.size()).mapToObj(i -> grid.getRow(i).getName()).toList();
    }

    private void selectRow(Long id) {
        var grid = test(grid());
        for (int i = 0; i < grid.size(); i++) {
            if (grid.getRow(i).getId().equals(id)) {
                grid.select(i);
                return;
            }
        }
        throw new AssertionError("Row not found: " + id);
    }
}
