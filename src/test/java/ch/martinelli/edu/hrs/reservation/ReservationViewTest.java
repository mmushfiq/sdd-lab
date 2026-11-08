package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional
@WithMockUser(roles = "RECEPTIONIST")
class ReservationViewTest extends HrsBrowserlessTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private Clock clock;

    private LocalDate today;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        var type = roomTypeService.save(roomType("Test Double", 2, "120.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T801");
        room.setRoomTypeId(type.getId());
        roomService.save(room);
    }

    @Test
    void searchShowsAvailableRoomsWithTotalPrice() {
        navigate(ReservationView.class);

        search(today.plusDays(1), today.plusDays(3), 2);

        var grid = test(grid());
        var row = rowOf("T801");
        assertThat(grid.getRow(row).roomTypeName()).isEqualTo("Test Double");
        assertThat(grid.getRow(row).totalPrice()).isEqualByComparingTo("240.00");
    }

    @Test
    void bookingShowsReservationNumber() {
        navigate(ReservationView.class);
        search(today.plusDays(1), today.plusDays(3), 2);

        test(grid()).select(rowOf("T801"));
        test($(Button.class).withText("Book").single()).click();
        test($(TextField.class).withLabel("Guest name").single()).setValue("Test Guest");
        test($(EmailField.class).withLabel("Email").single()).setValue("guest@example.com");
        test($(TextField.class).withLabel("Phone").single()).setValue("+41 79 000 00 00");
        test($(Button.class).withText("Confirm booking").single()).click();

        var number = dsl.select(RESERVATION.RESERVATION_NUMBER).from(RESERVATION)
                .where(RESERVATION.GUEST_NAME.eq("Test Guest")).fetchSingle().value1();
        assertThat(test($(Notification.class).last()).getText()).isEqualTo("Reservation " + number + " booked.");
        assertThat(rowOf("T801")).isNegative();
        // 002 US4 scenario 3: reception bookings send no confirmation email
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void bookingUsesTheSearchedStayEvenIfFieldsChangedAfterwards() {
        navigate(ReservationView.class);
        search(today.plusDays(1), today.plusDays(3), 2);

        test($(DatePicker.class).withLabel("Departure").single()).setValue(today.plusDays(9));
        test($(IntegerField.class).withLabel("Guests").single()).clear();
        test(grid()).select(rowOf("T801"));
        test($(Button.class).withText("Book").single()).click();
        test($(TextField.class).withLabel("Guest name").single()).setValue("Test Searched");
        test($(EmailField.class).withLabel("Email").single()).setValue("guest@example.com");
        test($(TextField.class).withLabel("Phone").single()).setValue("+41 79 000 00 00");
        test($(Button.class).withText("Confirm booking").single()).click();

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.GUEST_NAME.eq("Test Searched"));
        assertThat(stored.getArrivalDate()).isEqualTo(today.plusDays(1));
        assertThat(stored.getDepartureDate()).isEqualTo(today.plusDays(3));
        assertThat(stored.getNumberOfGuests()).isEqualTo(2);
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("240.00");
    }

    @Test
    void bookingFormRequiresGuestDetails() {
        navigate(ReservationView.class);
        search(today.plusDays(1), today.plusDays(3), 2);

        test(grid()).select(rowOf("T801"));
        test($(Button.class).withText("Book").single()).click();
        test($(EmailField.class).withLabel("Email").single()).setValue("not-an-email");
        test($(Button.class).withText("Confirm booking").single()).click();

        assertThat($(TextField.class).withLabel("Guest name").single().isInvalid()).isTrue();
        assertThat($(EmailField.class).withLabel("Email").single().isInvalid()).isTrue();
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.GUEST_EMAIL.eq("not-an-email"))).isZero();
    }

    @Test
    void noRoomAvailableMessage() {
        navigate(ReservationView.class);

        search(today.plusDays(1), today.plusDays(2), 50);

        assertThat($(Paragraph.class).withText("No room is available for these dates and number of guests.")
                .single().isVisible()).isTrue();
    }

    @Test
    void invalidDatesShowError() {
        navigate(ReservationView.class);

        search(today.plusDays(3), today.plusDays(1), 2);

        assertThat(test($(Notification.class).last()).getText()).isEqualTo("Departure must be after arrival.");
    }

    @Test
    void findAndChangeReservation() {
        var reservation = bookForGuest("Test Change Guest");
        navigate(ReservationView.class);

        findAndSelect("change guest");
        test($(Button.class).withText("Change").single()).click();
        test($(DatePicker.class).id("change-departure")).setValue(today.plusDays(4));
        test($(Button.class).withText("Save changes").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Reservation " + reservation.getReservationNumber() + " changed.");
        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId()));
        assertThat(stored.getDepartureDate()).isEqualTo(today.plusDays(4));
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("360.00");
    }

    @Test
    void changeIntoBookedNightsShowsError() {
        bookForGuest("Test Blocking Guest", today.plusDays(5), today.plusDays(7));
        bookForGuest("Test Moving Guest");
        navigate(ReservationView.class);

        findAndSelect("moving guest");
        test($(Button.class).withText("Change").single()).click();
        test($(DatePicker.class).id("change-departure")).setValue(today.plusDays(6));
        test($(Button.class).withText("Save changes").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("The room is no longer available for these dates.");
    }

    @Test
    void cancelAfterConfirmation() {
        var reservation = bookForGuest("Test Cancel Guest");
        navigate(ReservationView.class);

        findAndSelect(reservation.getReservationNumber());
        test($(Button.class).withText("Cancel reservation").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED.name());
        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Reservation " + reservation.getReservationNumber() + " cancelled.");
    }

    @Test
    void checkedInReservationCannotBeChangedOrCancelled() {
        var reservation = bookForGuest("Test Checked In Guest", today, today.plusDays(1));
        reservationService.checkIn(reservation.getId());
        navigate(ReservationView.class);

        findAndSelect("checked in guest");

        assertThat($(Button.class).withText("Change").single().isEnabled()).isFalse();
        assertThat($(Button.class).withText("Cancel reservation").single().isEnabled()).isFalse();
    }

    @Test
    void foundReservationsShowTheOnlineCancellationTime() {
        var future = bookForGuest("Test Future Guest", today.plusDays(5), today.plusDays(7));
        var passed = bookForGuest("Test Passed Guest", today.plusDays(1), today.plusDays(2));
        var cancelled = bookForGuest("Test Cancelled Guest", today.plusDays(10), today.plusDays(12));
        reservationService.cancel(cancelled.getId());
        navigate(ReservationView.class);

        assertThat(onlineCancellationOf(future.getReservationNumber()))
                .isEqualTo(CancellationDeadline.of(today.plusDays(5), 48).text());
        assertThat(onlineCancellationOf(passed.getReservationNumber()))
                .isEqualTo(CancellationDeadline.of(today.plusDays(1), 48).passedTimeText());
        assertThat(onlineCancellationOf(cancelled.getReservationNumber())).isEmpty();
    }

    @Test
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(ReservationView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerIsDenied() {
        assertThat(opens(ReservationView.class)).isFalse();
    }

    private ReservationRecord bookForGuest(String guest) {
        return bookForGuest(guest, today.plusDays(1), today.plusDays(3));
    }

    private ReservationRecord bookForGuest(String guest, LocalDate arrival,
                                                                                LocalDate departure) {
        var roomId = dsl.select(ROOM.ID).from(ROOM)
                .where(ROOM.ROOM_NUMBER.eq("T801")).fetchSingle().value1();
        return reservationService.book(new BookingRequest(roomId, arrival, departure, 2, guest,
                "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION));
    }

    @SuppressWarnings("unchecked")
    private void findAndSelect(String query) {
        test($(TextField.class).withLabel("Reservation number or guest name").single()).setValue(query);
        test($(Button.class).withText("Find").single()).click();
        Grid<FoundReservation> foundGrid = $(Grid.class).id("found");
        test(foundGrid).select(0);
    }

    @SuppressWarnings("unchecked")
    private String onlineCancellationOf(String reservationNumber) {
        test($(TextField.class).withLabel("Reservation number or guest name").single()).setValue(reservationNumber);
        test($(Button.class).withText("Find").single()).click();
        Grid<FoundReservation> foundGrid = $(Grid.class).id("found");
        var column = foundGrid.getColumns().indexOf(foundGrid.getColumns().stream()
                .filter(c -> "Online cancellation".equals(c.getHeaderText())).findFirst().orElseThrow());
        return test(foundGrid).getCellText(0, column);
    }

    private void search(LocalDate arrival, LocalDate departure, int guests) {
        test($(DatePicker.class).withLabel("Arrival").single()).setValue(arrival);
        test($(DatePicker.class).withLabel("Departure").single()).setValue(departure);
        test($(IntegerField.class).withLabel("Guests").single()).setValue(guests);
        test($(Button.class).withText("Search").single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<AvailableRoom> grid() {
        return $(Grid.class).id("available");
    }

    private int rowOf(String roomNumber) {
        var grid = test(grid());
        return IntStream.range(0, grid.size())
                .filter(i -> grid.getRow(i).roomNumber().equals(roomNumber))
                .findFirst().orElse(-1);
    }
}
