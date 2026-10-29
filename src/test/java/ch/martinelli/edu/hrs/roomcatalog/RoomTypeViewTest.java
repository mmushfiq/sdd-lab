package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RoomTypeViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomCatalogService roomCatalogService;

    @BeforeEach
    void navigateToView() {
        navigate(RoomTypeView.class);
    }

    @Test
    void listsRoomTypesSortedByName() {
        var grid = test(grid());

        assertThat(grid.size()).isEqualTo(3);
        assertThat(grid.getCellText(0, 0)).isEqualTo("Double");
        assertThat(grid.getCellText(0, 1)).isEqualTo("2");
        assertThat(grid.getCellText(0, 2)).isEqualTo("150.00");
        assertThat(grid.getCellText(1, 0)).isEqualTo("Family");
        assertThat(grid.getCellText(2, 0)).isEqualTo("Single");
    }

    @Test
    void createsRoomType() {
        fillForm("Suite", 2, "150.00");
        clickButton("Save");

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(4);
        assertThat(grid.getCellText(3, 0)).isEqualTo("Suite");
        assertThat(grid.getCellText(3, 1)).isEqualTo("2");
        assertThat(grid.getCellText(3, 2)).isEqualTo("150.00");
    }

    @Test
    void trimsName() {
        fillForm("  Suite  ", 2, "150.00");
        clickButton("Save");

        assertThat(test(grid()).getCellText(3, 0)).isEqualTo("Suite");
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        fillForm("double", 2, "150.00");
        clickButton("Save");

        var name = nameField();
        assertThat(name.isInvalid()).isTrue();
        assertThat(name.getErrorMessage()).isEqualTo("Name already exists");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void requiresName(String name) {
        fillForm(name, 2, "150.00");
        clickButton("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo("Name is required");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void rejectsCapacityOutOfRange(int capacity) {
        fillForm("Suite", capacity, "150.00");
        clickButton("Save");

        var field = $view(IntegerField.class).withLabel("Capacity").single();
        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo("Capacity must be between 1 and 10");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10.00", "150.001"})
    void rejectsInvalidRate(String rate) {
        fillForm("Suite", 2, rate);
        clickButton("Save");

        assertThat(rateField().isInvalid()).isTrue();
        assertThat(rateField().getErrorMessage()).startsWith("Nightly rate");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @Test
    void updatesRate() {
        test(grid()).select(0);
        test(rateField()).setValue(new BigDecimal("165.50"));
        clickButton("Save");

        var grid = test(grid());
        assertThat(grid.getCellText(0, 0)).isEqualTo("Double");
        assertThat(grid.getCellText(0, 2)).isEqualTo("165.50");
    }

    @Test
    void deletesUnusedRoomTypeAfterConfirmation() {
        createSuite();
        test(grid()).select(3);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @Test
    void keepsRoomTypeWhenDeleteIsCancelled() {
        createSuite();
        test(grid()).select(3);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(test(grid()).size()).isEqualTo(4);
    }

    @Test
    void refusesToDeleteRoomTypeUsedByRooms() {
        test(grid()).select(0);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(notificationText()).isEqualTo("Room type is used by rooms and cannot be deleted");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    @Test
    void reportsRoomTypeDeletedMeanwhile() {
        createSuite();
        test(grid()).select(3);
        roomCatalogService.deleteRoomType(test(grid()).getRow(3).getId());

        clickButton("Delete");
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(notificationText()).isEqualTo("Room type no longer exists");
        assertThat(test(grid()).size()).isEqualTo(3);
    }

    private void createSuite() {
        fillForm("Suite", 2, "300.00");
        clickButton("Save");
    }

    private void fillForm(String name, int capacity, String rate) {
        clickButton("New");
        test(nameField()).setValue(name);
        test($view(IntegerField.class).withLabel("Capacity").single()).setValue(capacity);
        test(rateField()).setValue(new BigDecimal(rate));
    }

    private void clickButton(String text) {
        test($view(Button.class).withText(text).single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomTypeRecord> grid() {
        return $view(Grid.class).single();
    }

    private TextField nameField() {
        return $view(TextField.class).withLabel("Name").single();
    }

    private BigDecimalField rateField() {
        return $view(BigDecimalField.class).withLabel("Nightly rate (CHF)").single();
    }

    private String notificationText() {
        return test($(Notification.class).single()).getText();
    }
}
