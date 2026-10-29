package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReservationListViewTest extends HrsBrowserlessTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    private String mueller;
    private String meier;
    private String rossi;

    @BeforeEach
    void createReservationsAndNavigate() {
        meier = book("104", 5, 7, "Hans Meier");
        mueller = book("101", 1, 3, "Anna Müller");
        rossi = book("105", 2, 4, "Carla Rossi");
        dsl.update(RESERVATION).set(RESERVATION.STATUS, "CANCELLED")
                .where(RESERVATION.BOOKING_NUMBER.eq(rossi)).execute();
        navigate(ReservationListView.class);
    }

    @Test
    void listsUpcomingConfirmedReservationsSortedByArrival() {
        var grid = test(grid());

        assertThat(grid.size()).isEqualTo(2);
        assertThat(grid.getCellText(0, 0)).isEqualTo(mueller);
        assertThat(grid.getCellText(0, 1)).isEqualTo("Anna Müller");
        assertThat(grid.getCellText(0, 2)).isEqualTo("101");
        assertThat(grid.getCellText(0, 3)).isEqualTo(day(1).toString());
        assertThat(grid.getCellText(0, 4)).isEqualTo(day(3).toString());
        assertThat(grid.getCellText(0, 5)).isEqualTo("1");
        assertThat(grid.getCellText(0, 6)).isEqualTo("240.00");
        assertThat(grid.getCellText(0, 7)).isEqualTo("CONFIRMED");
        assertThat(grid.getCellText(1, 0)).isEqualTo(meier);
    }

    @Test
    void filtersByBookingNumberInAnyStatus() {
        test(textField("Booking number")).setValue(rossi.toLowerCase());

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 0)).isEqualTo(rossi);
        assertThat(grid.getCellText(0, 7)).isEqualTo("CANCELLED");
    }

    @Test
    void filtersByPartOfGuestNameIgnoringCase() {
        test(textField("Guest name")).setValue("mül");

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 1)).isEqualTo("Anna Müller");
    }

    @Test
    void filtersByStaysOverlappingDateRange() {
        test($view(DatePicker.class).withLabel("From").single()).setValue(day(6));
        test($view(DatePicker.class).withLabel("To").single()).setValue(day(8));

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(1);
        assertThat(grid.getCellText(0, 0)).isEqualTo(meier);
    }

    @Test
    void showCancelledIncludesCancelledReservations() {
        test($view(Checkbox.class).withLabel("Show cancelled").single()).click();

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(3);
        assertThat(grid.getCellText(1, 0)).isEqualTo(rossi);
    }

    @Test
    void showsMessageWhenNothingMatches() {
        assertThat($view(Span.class).withText("No reservations found").exists()).isFalse();

        test(textField("Guest name")).setValue("zzz");

        assertThat(test(grid()).size()).isZero();
        assertThat(noReservations().isVisible()).isTrue();
    }

    // 003-reservation: list actions

    @Test
    void actionsFollowTheSelectedReservation() {
        test(grid()).select(0);
        assertThat(button("Change").isEnabled()).isTrue();
        assertThat(button("Cancel reservation").isEnabled()).isTrue();
        assertThat(button("Anonymize").isEnabled()).isFalse();

        test($view(Checkbox.class).withLabel("Show cancelled").single()).click();
        test(grid()).select(1);
        assertThat(test(grid()).getRow(1).bookingNumber()).isEqualTo(rossi);
        assertThat(button("Change").isEnabled()).isFalse();
        assertThat(button("Cancel reservation").isEnabled()).isFalse();
        assertThat(button("Anonymize").isEnabled()).isTrue();
    }

    @Test
    void cancelsAfterYes() {
        test(grid()).select(0);
        test(button("Cancel reservation")).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(lastNotification()).isEqualTo("Reservation cancelled");
        assertThat(status(mueller)).isEqualTo("CANCELLED");
        assertThat(test(grid()).size()).isEqualTo(1);
    }

    @Test
    void keepsReservationWhenCancelIsAnsweredNo() {
        test(grid()).select(0);
        test(button("Cancel reservation")).click();
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(status(mueller)).isEqualTo("CONFIRMED");
        assertThat(test(grid()).size()).isEqualTo(2);
    }

    @Test
    void anonymizesAfterYesAndShowsItAsAnonymized() {
        test(textField("Booking number")).setValue(rossi);
        test(grid()).select(0);
        test(button("Anonymize")).click();
        test($(ConfirmDialog.class).single()).confirm();

        var grid = test(grid());
        assertThat(grid.getCellText(0, 1)).isEqualTo("Anonymized");
        assertThat(grid.getCellText(0, 7)).isEqualTo("CANCELLED (anonymized)");
        grid.select(0);
        assertThat(button("Anonymize").isEnabled()).isFalse();
        assertThat(button("Change").isEnabled()).isFalse();
    }

    @Test
    void keepsGuestDataWhenAnonymizeIsAnsweredNo() {
        test(textField("Booking number")).setValue(rossi);
        test(grid()).select(0);
        test(button("Anonymize")).click();
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(test(grid()).getCellText(0, 1)).isEqualTo("Carla Rossi");
    }

    @Test
    void changesDatesInTheDialog() {
        test(grid()).select(1);
        test(button("Change")).click();
        test(inDialog(DatePicker.class, "Departure")).setValue(day(8));
        clickInDialog("Save");

        assertThat(lastNotification()).isEqualTo("Reservation changed");
        assertThat($(Dialog.class).exists()).isFalse();
        var grid = test(grid());
        assertThat(grid.getCellText(1, 0)).isEqualTo(meier);
        assertThat(grid.getCellText(1, 4)).isEqualTo(day(8).toString());
        assertThat(grid.getCellText(1, 6)).isEqualTo("450.00");
    }

    @Test
    void reportsUnavailableRoomAndKeepsTheDialogOpen() {
        test(grid()).select(1);
        test(button("Change")).click();
        test(inDialog(DatePicker.class, "Arrival")).setValue(day(2));
        test(inDialog(DatePicker.class, "Departure")).setValue(day(4));
        test(inDialog(ComboBox.class, "Room")).selectItem("101 (Single)");
        clickInDialog("Save");

        assertThat(lastNotification()).isEqualTo("Room is not available for these dates");
        assertThat($(Dialog.class).exists()).isTrue();
        assertThat(test(grid()).getCellText(1, 2)).isEqualTo("104");
    }

    @Test
    void showsCapacityErrorInTheDialog() {
        test(grid()).select(1);
        test(button("Change")).click();
        test(inDialog(IntegerField.class, "Guests")).setValue(3);
        clickInDialog("Save");

        var guests = inDialog(IntegerField.class, "Guests");
        assertThat(guests.isInvalid()).isTrue();
        assertThat(guests.getErrorMessage()).isEqualTo("This room allows at most 2 guests");
    }

    private Button button(String text) {
        return $view(Button.class).withText(text).single();
    }

    private void clickInDialog(String text) {
        test($(Button.class).from($(Dialog.class).single()).withText(text).single()).click();
    }

    private <T extends com.vaadin.flow.component.Component> T inDialog(Class<T> type, String label) {
        return $(type).from($(Dialog.class).single()).withLabel(label).single();
    }

    private String lastNotification() {
        return test($(Notification.class).last()).getText();
    }

    private String status(String bookingNumber) {
        return dsl.select(RESERVATION.STATUS).from(RESERVATION).where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber))
                .fetchSingle(RESERVATION.STATUS);
    }

    private String book(String roomNumber, int arrivalInDays, int departureInDays, String name) {
        long roomId = dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq(roomNumber)).fetchSingle(ROOM.ID);
        return reservationService.createReservation(roomId, new StayPeriod(day(arrivalInDays), day(departureInDays)),
                1, new Guest(name, "guest@example.com", "000"));
    }

    private TextField textField(String label) {
        return $view(TextField.class).withLabel(label).single();
    }

    private Span noReservations() {
        return $view(Span.class).withText("No reservations found").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<ReservationListItem> grid() {
        return $view(Grid.class).single();
    }

    private static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }
}
