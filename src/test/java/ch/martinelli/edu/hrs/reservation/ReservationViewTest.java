package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.button.Button;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@WithMockUser(roles = "RECEPTIONIST")
class ReservationViewTest extends HrsBrowserlessTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

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
    @WithMockUser(roles = "HOUSEKEEPING")
    void housekeepingIsDenied() {
        assertThat(opens(ReservationView.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerIsDenied() {
        assertThat(opens(ReservationView.class)).isFalse();
    }

    private void search(LocalDate arrival, LocalDate departure, int guests) {
        test($(DatePicker.class).withLabel("Arrival").single()).setValue(arrival);
        test($(DatePicker.class).withLabel("Departure").single()).setValue(departure);
        test($(IntegerField.class).withLabel("Guests").single()).setValue(guests);
        test($(Button.class).withText("Search").single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<AvailableRoom> grid() {
        return $(Grid.class).single();
    }

    private int rowOf(String roomNumber) {
        var grid = test(grid());
        return IntStream.range(0, grid.size())
                .filter(i -> grid.getRow(i).roomNumber().equals(roomNumber))
                .findFirst().orElse(-1);
    }
}
