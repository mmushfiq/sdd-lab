package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.CancellationDeadline;
import ch.martinelli.edu.hrs.reservation.ReservationService;
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
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Runs without a logged-in user: the guest page is public (FR-023).
 */
@Transactional
class GuestBookingViewTest extends HrsBrowserlessTest {

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    private LocalDate today;
    private long roomId;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        var type = roomTypeService.save(roomType("Test Online Double", 2, "120.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T811");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @Test
    void searchShowsRoomTypeCapacityAndTotalPrice() {
        navigate(GuestBookingView.class);

        search(today.plusDays(1), today.plusDays(3), 2);

        var row = test(grid()).getRow(rowOf("T811"));
        assertThat(row.roomTypeName()).isEqualTo("Test Online Double");
        assertThat(row.capacity()).isEqualTo(2);
        assertThat(row.totalPrice()).isEqualByComparingTo("240.00");
    }

    @Test
    void onlineBookingShowsReservationNumber() {
        navigate(GuestBookingView.class);
        search(today.plusDays(1), today.plusDays(3), 2);

        openBooking();
        fillGuest("Test Online Guest", "online@example.com");
        test($(Button.class).withText("Confirm booking").single()).click();

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.GUEST_NAME.eq("Test Online Guest"));
        assertThat(stored.getStatus()).isEqualTo("BOOKED");
        assertThat(stored.getSource()).isEqualTo("ONLINE");
        assertThat($(Paragraph.class).withText("Your room is booked. Your reservation number is "
                + stored.getReservationNumber() + ". Use it with your email under "
                + "\"View or cancel my reservation\".").single().isVisible()).isTrue();
        assertThat(rowOf("T811")).isNegative();
    }

    @Test
    void onlineBookingSendsOneConfirmationEmail() {
        navigate(GuestBookingView.class);
        search(today.plusDays(5), today.plusDays(7), 2);
        openBooking();
        fillGuest("Test Mail Guest", "mail@example.com");
        test($(Button.class).withText("Confirm booking").single()).click();

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.GUEST_NAME.eq("Test Mail Guest"));
        assertThat(captor.getValue().getTo()).containsExactly("mail@example.com");
        assertThat(captor.getValue().getText()).contains(stored.getReservationNumber(), "Test Online Double",
                CancellationDeadline.of(today.plusDays(5), 48).text());
    }

    @Test
    void mailFailureKeepsTheBookingAndShowsTheConfirmation() {
        doThrow(new MailSendException("mail server down")).when(mailSender).send(any(SimpleMailMessage.class));
        navigate(GuestBookingView.class);
        search(today.plusDays(5), today.plusDays(7), 2);
        openBooking();
        fillGuest("Test No Mail Guest", "nomail@example.com");
        test($(Button.class).withText("Confirm booking").single()).click();

        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.GUEST_NAME.eq("Test No Mail Guest"));
        assertThat(stored.getStatus()).isEqualTo("BOOKED");
        assertThat($(Paragraph.class).withText("Your room is booked. Your reservation number is "
                + stored.getReservationNumber() + ". Use it with your email under "
                + "\"View or cancel my reservation\".").single().isVisible()).isTrue();
    }

    @Test
    void linksToMyReservation() {
        navigate(GuestBookingView.class);

        test($(RouterLink.class).withText("View or cancel my reservation").single()).click();

        assertThat(getCurrentView()).isInstanceOf(MyReservationView.class);
    }

    @Test
    void missingNameAndInvalidEmailAreMarked() {
        navigate(GuestBookingView.class);
        search(today.plusDays(1), today.plusDays(3), 2);

        openBooking();
        test($(EmailField.class).withLabel("Email").single()).setValue("not-an-email");
        test($(Button.class).withText("Confirm booking").single()).click();

        assertThat($(TextField.class).withLabel("Guest name").single().isInvalid()).isTrue();
        assertThat($(EmailField.class).withLabel("Email").single().isInvalid()).isTrue();
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(roomId))).isZero();
    }

    @Test
    void roomTakenMeanwhileShowsNoLongerAvailable() {
        navigate(GuestBookingView.class);
        search(today.plusDays(1), today.plusDays(3), 2);
        openBooking();
        fillGuest("Test Late Guest", "late@example.com");

        reservationService.book(new BookingRequest(roomId, today.plusDays(2), today.plusDays(4), 1,
                "Test Faster Guest", "fast@example.com", "+41 79 000 00 01", BookingSource.RECEPTION));
        test($(Button.class).withText("Confirm booking").single()).click();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("The room is no longer available for these dates.");
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.GUEST_NAME.eq("Test Late Guest"))).isZero();
        assertThat(rowOf("T811")).isNegative();
    }

    private void search(LocalDate arrival, LocalDate departure, int guests) {
        test($(DatePicker.class).withLabel("Arrival").single()).setValue(arrival);
        test($(DatePicker.class).withLabel("Departure").single()).setValue(departure);
        test($(IntegerField.class).withLabel("Guests").single()).setValue(guests);
        test($(Button.class).withText("Search").single()).click();
    }

    private void openBooking() {
        test(grid()).select(rowOf("T811"));
        test($(Button.class).withText("Book").single()).click();
    }

    private void fillGuest(String name, String email) {
        test($(TextField.class).withLabel("Guest name").single()).setValue(name);
        test($(EmailField.class).withLabel("Email").single()).setValue(email);
        test($(TextField.class).withLabel("Phone").single()).setValue("+41 79 000 00 00");
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
