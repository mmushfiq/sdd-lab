package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reservation.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationTestData;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.reservation.ReservationTestData.stay;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RoomViewTest extends HrsBrowserlessTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void navigateToView() {
        navigate(RoomView.class);
    }

    @Test
    void listsRoomsSortedByNumberWithTypeDetails() {
        var grid = test(grid());

        assertThat(grid.size()).isEqualTo(10);
        assertThat(grid.getCellText(0, 0)).isEqualTo("101");
        assertThat(grid.getCellText(0, 1)).isEqualTo("Single");
        assertThat(grid.getCellText(0, 2)).isEqualTo("1");
        assertThat(grid.getCellText(0, 3)).isEqualTo("120.00");
        assertThat(grid.getCellText(0, 4)).isEqualTo("In service");
        assertThat(grid.getCellText(9, 0)).isEqualTo("110");
    }

    @Test
    void createsRoom() {
        clickButton("New");
        test(numberField()).setValue(" 111 ");
        test(roomTypeField()).selectItem("Double");
        clickButton("Save");

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(11);
        assertThat(grid.getCellText(10, 0)).isEqualTo("111");
        assertThat(grid.getCellText(10, 1)).isEqualTo("Double");
    }

    @Test
    void rejectsDuplicateRoomNumber() {
        clickButton("New");
        test(numberField()).setValue("101");
        test(roomTypeField()).selectItem("Double");
        clickButton("Save");

        assertThat(numberField().isInvalid()).isTrue();
        assertThat(numberField().getErrorMessage()).isEqualTo("Room number already exists");
        assertThat(test(grid()).size()).isEqualTo(10);
    }

    @Test
    void rejectsRoomNumberLongerThanTenCharacters() {
        clickButton("New");
        test(numberField()).setValue("12345678901");
        test(roomTypeField()).selectItem("Double");
        clickButton("Save");

        assertThat(numberField().isInvalid()).isTrue();
        assertThat(numberField().getErrorMessage()).isEqualTo("Room number can have at most 10 characters");
        assertThat(test(grid()).size()).isEqualTo(10);
    }

    @Test
    void requiresRoomType() {
        clickButton("New");
        test(numberField()).setValue("112");
        clickButton("Save");

        assertThat(roomTypeField().isInvalid()).isTrue();
        assertThat(roomTypeField().getErrorMessage()).isEqualTo("Room type is required");
        assertThat(test(grid()).size()).isEqualTo(10);
    }

    @Test
    void changesRoomType() {
        test(grid()).select(0);
        test(roomTypeField()).selectItem("Double");
        clickButton("Save");

        assertThat(test(grid()).getCellText(0, 1)).isEqualTo("Double");
    }

    @Test
    void deletesRoomAfterConfirmation() {
        test(grid()).select(0);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).confirm();

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(9);
        assertThat(grid.getCellText(0, 0)).isEqualTo("102");
    }

    @Test
    void keepsRoomWhenDeleteIsCancelled() {
        test(grid()).select(0);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(test(grid()).size()).isEqualTo(10);
    }

    @Test
    void setsRoomOutOfServiceAndBackInService() {
        test(grid()).select(0);
        test(outOfServiceField()).click();
        clickButton("Save");
        assertThat(test(grid()).getCellText(0, 4)).isEqualTo("Out of service");

        test(grid()).select(0);
        test(outOfServiceField()).click();
        clickButton("Save");
        assertThat(test(grid()).getCellText(0, 4)).isEqualTo("In service");
    }

    // 002-reservation / 006 protect rooms with reservations

    @Test
    void refusesToDeleteRoomWithReservations() {
        book101();
        test(grid()).select(0);
        clickButton("Delete");
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(lastNotification())
                .isEqualTo("Room has reservations and cannot be deleted; set it out of service instead");
        assertThat(test(grid()).size()).isEqualTo(10);
    }

    @Test
    void refusesOutOfServiceWithFutureReservationsAndListsThem() {
        var bookingNumber = book101();
        test(grid()).select(0);
        test(outOfServiceField()).click();
        clickButton("Save");

        assertThat(lastNotification())
                .isEqualTo("Room has future reservations and cannot be set out of service: " + bookingNumber);
        assertThat(test(grid()).getCellText(0, 4)).isEqualTo("In service");
    }

    private String book101() {
        var data = new ReservationTestData(dsl);
        return reservationService.createReservation(data.roomId("101"), stay(3, 5), 1,
                new Guest("Anna Müller", "anna@example.com", "000"));
    }

    private String lastNotification() {
        return test($(Notification.class).last()).getText();
    }

    private void clickButton(String text) {
        test($view(Button.class).withText(text).single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomListItem> grid() {
        return $view(Grid.class).single();
    }

    private TextField numberField() {
        return $view(TextField.class).withLabel("Room number").single();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Object> roomTypeField() {
        return $view(ComboBox.class).withLabel("Room type").single();
    }

    private Checkbox outOfServiceField() {
        return $view(Checkbox.class).withLabel("Out of service").single();
    }
}
