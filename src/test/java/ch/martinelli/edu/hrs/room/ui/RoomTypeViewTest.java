package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithUserDetails("manager")
class RoomTypeViewTest extends HrsBrowserlessTest {

    Grid<RoomTypeRecord> grid;
    TextField name;
    IntegerField capacity;
    BigDecimalField nightlyRate;

    @SuppressWarnings("unchecked")
    private void open() {
        navigate(RoomTypeView.class);
        grid = $(Grid.class).id("room-types");
        name = $(TextField.class).withLabel("Name").single();
        capacity = $(IntegerField.class).withLabel("Capacity").single();
        nightlyRate = $(BigDecimalField.class).withLabel("Nightly rate").single();
    }

    // Scenario "Manager lists room types"
    @Test
    void lists_seeded_room_types() {
        open();
        assertThat(rows()).containsExactly("Double|2|CHF 140.00", "Family|4|CHF 220.00", "Single|1|CHF 90.00");
    }

    // Scenario "Room type created"
    @Test
    void creates_suite() {
        open();
        fill("Suite", 3, "250.00");
        save();

        assertThat(rows()).contains("Suite|3|CHF 250.00");
    }

    // Scenario "Nightly rate changed"
    @Test
    void changes_nightly_rate() {
        open();
        test(grid).select(0);
        assertThat(name.getValue()).isEqualTo("Double");
        nightlyRate.setValue(new BigDecimal("150.00"));
        save();

        assertThat(rows()).contains("Double|2|CHF 150.00");
    }

    // Scenario "Room type name already used"
    @Test
    void duplicate_name_shows_message() {
        open();
        fill("Double", 2, "100.00");
        save();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("A room type with this name already exists");
        assertThat(test(grid).size()).isEqualTo(3);
    }

    // Scenario "Changed name already used"
    @Test
    void changed_name_already_used_shows_message() {
        open();
        test(grid).select(2);
        test(name).setValue("Double");
        save();

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("A room type with this name already exists");
        assertThat(rows()).contains("Single|1|CHF 90.00");
    }

    // Scenario "Missing or invalid room type values"
    @Test
    void invalid_values_mark_the_fields() {
        open();
        fill("", 0, "0.00");
        save();

        assertThat(name.isInvalid()).isTrue();
        assertThat(name.getErrorMessage()).isEqualTo("Name is required");
        assertThat(capacity.isInvalid()).isTrue();
        assertThat(capacity.getErrorMessage()).isEqualTo("Capacity must be at least 1");
        assertThat(nightlyRate.isInvalid()).isTrue();
        assertThat(nightlyRate.getErrorMessage()).isEqualTo("Nightly rate must be greater than zero");
        assertThat(test(grid).size()).isEqualTo(3);
    }

    // Scenario "Missing or invalid room type values": a name of only spaces counts as empty
    @Test
    void blank_name_marks_the_field() {
        open();
        fill("   ", 2, "100.00");
        save();

        assertThat(name.isInvalid()).isTrue();
        assertThat(name.getErrorMessage()).isEqualTo("Name is required");
        assertThat(test(grid).size()).isEqualTo(3);
    }

    // Scenario "No delete action"
    @Test
    void offers_no_delete_action() {
        open();
        assertThat($(Button.class).withTextContaining("Delete").exists()).isFalse();
    }

    // Scenario "Receptionist opens the rooms page" (same rule for the room types page)
    @Test
    @WithUserDetails("reception")
    void receptionist_gets_access_denied() {
        navigate(RoomTypeView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
    }

    private void fill(String nameValue, int capacityValue, String rate) {
        test(name).setValue(nameValue);
        capacity.setValue(capacityValue);
        nightlyRate.setValue(new BigDecimal(rate));
    }

    private void save() {
        test($(Button.class).withText("Save").single()).click();
    }

    private List<String> rows() {
        return IntStream.range(0, test(grid).size())
                .mapToObj(i -> test(grid).getCellText(i, 0) + "|" + test(grid).getCellText(i, 1) + "|"
                        + test(grid).getCellText(i, 2))
                .toList();
    }
}
