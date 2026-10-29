package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
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
class AvailabilityViewTest extends HrsBrowserlessTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void navigateToView() {
        navigate(AvailabilityView.class);
    }

    @Test
    void searchShowsFreeRoomsWithTotalPrice() {
        search(1, 3, 2);

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(7);
        assertThat(grid.getCellText(0, 0)).isEqualTo("104");
        assertThat(grid.getCellText(0, 1)).isEqualTo("Double");
        assertThat(grid.getCellText(0, 2)).isEqualTo("2");
        assertThat(grid.getCellText(0, 3)).isEqualTo("150.00");
        assertThat(grid.getCellText(0, 4)).isEqualTo("300.00");
    }

    @Test
    void rejectsArrivalInThePast() {
        search(-1, 2, 1);

        var arrival = $view(DatePicker.class).withLabel("Arrival").single();
        assertThat(arrival.isInvalid()).isTrue();
        assertThat(arrival.getErrorMessage()).isEqualTo("Arrival must be today or later");
        assertThat(test(grid()).size()).isZero();
    }

    @Test
    void rejectsDepartureNotAfterArrival() {
        search(3, 3, 1);

        var departure = $view(DatePicker.class).withLabel("Departure").single();
        assertThat(departure.isInvalid()).isTrue();
        assertThat(departure.getErrorMessage()).isEqualTo("Departure must be after arrival");
        assertThat(test(grid()).size()).isZero();
    }

    @Test
    void rejectsZeroGuests() {
        search(1, 2, 0);

        assertThat(guestsInView().isInvalid()).isTrue();
        assertThat(test(grid()).size()).isZero();
    }

    @Test
    void showsMessageWhenNoRoomMatches() {
        search(1, 2, 5);

        assertThat(test(grid()).size()).isZero();
        assertThat($view(Span.class).withText("No rooms available for these dates").single().isVisible()).isTrue();
    }

    @Test
    void booksRoomAndShowsBookingNumber() {
        search(1, 3, 2);
        openBooking(0);
        fillGuest("Anna Müller", "anna@example.com", "+41 79 123 45 67");
        clickInDialog("Save");

        assertThat(lastNotification()).matches("Reservation confirmed: [ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        var bookingNumber = lastNotification().substring("Reservation confirmed: ".length());
        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.BOOKING_NUMBER.eq(bookingNumber));
        assertThat(stored.getStatus()).isEqualTo("CONFIRMED");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("300.00");
        assertThat($(Dialog.class).exists()).isFalse();
        assertThat(test(grid()).getCellText(0, 0)).isEqualTo("105");
    }

    @Test
    void requiresNameEmailAndPhone() {
        search(1, 3, 2);
        openBooking(0);
        clickInDialog("Save");

        assertThat(inDialog(TextField.class, "Name").getErrorMessage()).isEqualTo("Name is required");
        assertThat(inDialog(EmailField.class, "Email").getErrorMessage()).isEqualTo("Email is required");
        assertThat(inDialog(TextField.class, "Phone").getErrorMessage()).isEqualTo("Phone is required");
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void rejectsInvalidEmail() {
        search(1, 3, 2);
        openBooking(0);
        fillGuest("Anna Müller", "anna.example.com", "+41 79 123 45 67");
        clickInDialog("Save");

        var email = inDialog(EmailField.class, "Email");
        assertThat(email.isInvalid()).isTrue();
        assertThat(email.getErrorMessage()).isEqualTo("Enter a valid email address");
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void rejectsMoreGuestsThanRoomCapacity() {
        search(1, 3, 2);
        openBooking(0);
        test(inDialog(IntegerField.class, "Guests")).setValue(3);
        fillGuest("Anna Müller", "anna@example.com", "+41 79 123 45 67");
        clickInDialog("Save");

        var guests = inDialog(IntegerField.class, "Guests");
        assertThat(guests.isInvalid()).isTrue();
        assertThat(guests.getErrorMessage()).isEqualTo("This room allows at most 2 guests");
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void reportsRoomBookedByOthersSinceTheSearch() {
        search(1, 3, 2);
        long room104 = dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq("104")).fetchSingle(ROOM.ID);
        reservationService.createReservation(room104, new StayPeriod(day(1), day(3)), 2,
                new Guest("Other Guest", "other@example.com", "000"));

        openBooking(0);
        fillGuest("Anna Müller", "anna@example.com", "+41 79 123 45 67");
        clickInDialog("Save");

        assertThat(lastNotification()).isEqualTo("Room is no longer available for these dates");
        assertThat(dsl.fetchCount(RESERVATION)).isEqualTo(1);
        assertThat(test(grid()).getCellText(0, 0)).isEqualTo("105");
    }

    private void search(int arrivalInDays, int departureInDays, int guests) {
        test($view(DatePicker.class).withLabel("Arrival").single()).setValue(day(arrivalInDays));
        test($view(DatePicker.class).withLabel("Departure").single()).setValue(day(departureInDays));
        test(guestsInView()).setValue(guests);
        test($view(Button.class).withText("Search").single()).click();
    }

    private void openBooking(int row) {
        test((Button) test(grid()).getCellComponent(row, 5)).click();
    }

    private void fillGuest(String name, String email, String phone) {
        test(inDialog(TextField.class, "Name")).setValue(name);
        test(inDialog(EmailField.class, "Email")).setValue(email);
        test(inDialog(TextField.class, "Phone")).setValue(phone);
    }

    private void clickInDialog(String text) {
        test($(Button.class).from($(Dialog.class).single()).withText(text).single()).click();
    }

    private <T extends com.vaadin.flow.component.Component> T inDialog(Class<T> type, String label) {
        return $(type).from($(Dialog.class).single()).withLabel(label).single();
    }

    private IntegerField guestsInView() {
        return $view(IntegerField.class).withLabel("Guests").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<AvailableRoom> grid() {
        return $view(Grid.class).single();
    }

    private String lastNotification() {
        return test($(Notification.class).last()).getText();
    }

    private static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }
}
