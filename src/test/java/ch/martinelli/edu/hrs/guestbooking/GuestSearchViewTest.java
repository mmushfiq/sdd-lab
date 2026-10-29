package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.reservation.ReservationTestData;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
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
class GuestSearchViewTest extends HrsBrowserlessTest {

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void navigateToView() {
        navigate(GuestSearchView.class);
    }

    @Test
    void opensWithSearchFormAndNoStaffLinks() {
        assertThat($view(DatePicker.class).withLabel("Arrival").single()).isNotNull();
        assertThat($view(DatePicker.class).withLabel("Departure").single()).isNotNull();
        assertThat($view(IntegerField.class).withLabel("Guests").single()).isNotNull();
        assertThat($view(RouterLink.class).all()).extracting(RouterLink::getHref)
                .containsExactly("booking/reservation");
    }

    @Test
    void listsRoomTypesWithoutRoomNumbers() {
        search(1, 3, 2);

        var grid = test(grid());
        assertThat(grid.size()).isEqualTo(2);
        assertThat(grid.getCellText(0, 0)).isEqualTo("Double");
        assertThat(grid.getCellText(0, 1)).isEqualTo("2");
        assertThat(grid.getCellText(0, 2)).isEqualTo("150.00");
        assertThat(grid.getCellText(0, 3)).isEqualTo("300.00");
        assertThat(grid.getCellText(0, 4)).isEqualTo("5");
        assertThat(grid.getCellText(1, 0)).isEqualTo("Family");
        assertThat(grid().getColumns()).extracting(c -> c.getHeaderText())
                .doesNotContain("Room");
    }

    @Test
    void hidesFullyBookedRoomType() {
        var data = new ReservationTestData(dsl);
        var rooms = new String[]{"104", "105", "106", "107", "108"};
        for (int i = 0; i < rooms.length; i++) {
            data.insert("CCCCCCC" + (i + 2), rooms[i], 1, 3, ReservationStatus.CONFIRMED);
        }

        search(1, 3, 2);

        assertThat(test(grid()).size()).isEqualTo(1);
        assertThat(test(grid()).getCellText(0, 0)).isEqualTo("Family");
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
    }

    @Test
    void showsMessageWhenNothingIsFree() {
        search(1, 2, 5);

        assertThat(test(grid()).size()).isZero();
        assertThat($view(Span.class).withText("No rooms available for these dates").single().isVisible()).isTrue();
    }

    @Test
    void booksLowestRoomShowsConfirmationAndRefreshesCounts() {
        search(1, 3, 2);
        openBooking(0);
        assertThat(dialogText()).contains("We only store your name, email and phone");
        fillGuest("Anna Müller", "anna@example.com", "+41 79 123 45 67");
        clickInDialog("Book");

        var stored = dsl.select(ROOM.NUMBER, RESERVATION.STATUS, RESERVATION.TOTAL_PRICE)
                .from(RESERVATION).join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID)).fetchSingle();
        assertThat(stored.value1()).isEqualTo("104");
        assertThat(stored.value2()).isEqualTo("CONFIRMED");
        assertThat(stored.value3()).isEqualByComparingTo("300.00");
        var text = dialogText();
        assertThat($(Dialog.class).single().getHeaderTitle()).isEqualTo("Reservation confirmed");
        assertThat(text).contains("Double", "300.00", "Please keep your booking number");
        var bookingNumber = dsl.select(RESERVATION.BOOKING_NUMBER).from(RESERVATION)
                .fetchSingle(RESERVATION.BOOKING_NUMBER);
        assertThat(text).contains(bookingNumber);

        clickInDialog("Close");
        assertThat($(Dialog.class).exists()).isFalse();
        assertThat(test(grid()).getCellText(0, 4)).isEqualTo("4");
    }

    @Test
    void requiresNameEmailAndPhoneAndStoresNothing() {
        search(1, 3, 2);
        openBooking(0);
        clickInDialog("Book");

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
        clickInDialog("Book");

        assertThat(inDialog(EmailField.class, "Email").getErrorMessage()).isEqualTo("Enter a valid email address");
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void reportsRoomTypeTakenSinceTheSearch() {
        var data = new ReservationTestData(dsl);
        search(1, 3, 2);
        var rooms = new String[]{"104", "105", "106", "107", "108"};
        for (int i = 0; i < rooms.length; i++) {
            data.insert("DDDDDDD" + (i + 2), rooms[i], 1, 3, ReservationStatus.CONFIRMED);
        }
        var before = dsl.fetchCount(RESERVATION);

        openBooking(0);
        fillGuest("Anna Müller", "anna@example.com", "+41 79 123 45 67");
        clickInDialog("Book");

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("This room type is no longer available for these dates");
        assertThat(dsl.fetchCount(RESERVATION)).isEqualTo(before);
        assertThat($(Dialog.class).exists()).isFalse();
        assertThat(test(grid()).getCellText(0, 0)).isEqualTo("Family");
    }

    private void search(int arrivalInDays, int departureInDays, int guests) {
        test($view(DatePicker.class).withLabel("Arrival").single()).setValue(day(arrivalInDays));
        test($view(DatePicker.class).withLabel("Departure").single()).setValue(day(departureInDays));
        test($view(IntegerField.class).withLabel("Guests").single()).setValue(guests);
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

    private <T extends Component> T inDialog(Class<T> type, String label) {
        return $(type).from($(Dialog.class).single()).withLabel(label).single();
    }

    private String dialogText() {
        return $(Dialog.class).single().getElement().getTextRecursively();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomTypeAvailability> grid() {
        return $view(Grid.class).single();
    }

    private static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }
}
