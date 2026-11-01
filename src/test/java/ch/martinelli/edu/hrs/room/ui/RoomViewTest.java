package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("manager")
class RoomViewTest extends HrsBrowserlessTest {

    Grid<RoomRecord> grid;
    TextField roomNumber;
    ComboBox<RoomTypeRecord> roomType;

    @SuppressWarnings("unchecked")
    private void open() {
        navigate(RoomView.class);
        grid = $(Grid.class).id("rooms");
        roomNumber = $(TextField.class).withLabel("Room number").single();
        roomType = $(ComboBox.class).withLabel("Room type").single();
    }

    // Scenarios "Manager lists rooms" and "Ten rooms after installation"
    @Test
    void lists_ten_rooms_sorted_by_number() {
        open();

        assertThat(rows()).containsExactly(
                "101|Single|1|CHF 90.00", "102|Single|1|CHF 90.00", "103|Single|1|CHF 90.00",
                "104|Double|2|CHF 140.00", "105|Double|2|CHF 140.00", "106|Double|2|CHF 140.00",
                "107|Double|2|CHF 140.00", "108|Double|2|CHF 140.00",
                "109|Family|4|CHF 220.00", "110|Family|4|CHF 220.00");
    }

    // Scenario "Room created"
    @Test
    void creates_room_111_as_double() {
        open();
        test(roomNumber).setValue("111");
        test(roomType).selectItem("Double");
        save();

        assertThat(rows()).hasSize(11).last().isEqualTo("111|Double|2|CHF 140.00");
    }

    // Scenario "Room type of a room changed"
    @Test
    void changes_room_104_to_family() {
        open();
        test(grid).select(3);
        assertThat(roomNumber.getValue()).isEqualTo("104");
        test(roomType).selectItem("Family");
        save();

        assertThat(rows()).contains("104|Family|4|CHF 220.00");
    }

    // Scenario "Room number already used"
    @Test
    void duplicate_room_number_shows_message() {
        open();
        test(roomNumber).setValue("101");
        test(roomType).selectItem("Double");
        save();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("A room with this number already exists");
        assertThat(rows()).hasSize(10);
    }

    // Scenario "Changed room number already used"
    @Test
    void changed_room_number_already_used_shows_message() {
        open();
        test(grid).select(1);
        test(roomNumber).setValue("101");
        save();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("A room with this number already exists");
        assertThat(rows()).contains("102|Single|1|CHF 90.00");
    }

    // Scenario "Missing room values"
    @Test
    void missing_values_mark_the_fields() {
        open();
        save();

        assertThat(roomNumber.isInvalid()).isTrue();
        assertThat(roomNumber.getErrorMessage()).isEqualTo("Room number is required");
        assertThat(roomType.isInvalid()).isTrue();
        assertThat(roomType.getErrorMessage()).isEqualTo("Room type is required");
        assertThat(rows()).hasSize(10);
    }

    // Scenario "Missing room values": a room number of only spaces counts as missing
    @Test
    void blank_room_number_marks_the_field() {
        open();
        test(roomNumber).setValue("   ");
        test(roomType).selectItem("Double");
        save();

        assertThat(roomNumber.isInvalid()).isTrue();
        assertThat(roomNumber.getErrorMessage()).isEqualTo("Room number is required");
        assertThat(rows()).hasSize(10);
    }

    // Scenario "No delete action"
    @Test
    void offers_no_delete_action() {
        open();

        assertThat($(Button.class).withTextContaining("Delete").exists()).isFalse();
    }

    // Scenario "Receptionist opens the rooms page"
    @Test
    @WithUserDetails("reception")
    void receptionist_gets_access_denied() {
        navigate(RoomView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private List<String> rows() {
        return IntStream.range(0, test(grid).size())
                .mapToObj(i -> IntStream.range(0, 4).mapToObj(c -> test(grid).getCellText(i, c))
                        .reduce((a, b) -> a + "|" + b).orElseThrow())
                .toList();
    }
}
