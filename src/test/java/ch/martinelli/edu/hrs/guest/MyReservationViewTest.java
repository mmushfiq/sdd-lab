package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs without a logged-in user: the page is public (FR-023).
 */
@Transactional
class MyReservationViewTest extends HrsBrowserlessTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Clock clock;

    private LocalDate today;
    private long roomId;

    @BeforeEach
    void createRoom() {
        today = LocalDate.now(clock);
        var type = roomTypeService.save(roomType("Test My Double", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T761");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
    }

    @Test
    void showsDetailsAndCancelsAfterConfirmation() {
        var reservation = book(today.plusDays(5));
        navigate(MyReservationView.class);

        lookUp(reservation.getReservationNumber(), "my@example.com");

        assertThat(text("Test My Double")).isTrue();
        assertThat(text("200.00")).isTrue();
        assertThat(text("Booked")).isTrue();
        assertThat(text("Test My Guest")).isFalse();

        test($(Button.class).withText("Cancel reservation").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getStatus())
                .isEqualTo("CANCELLED");
        assertThat(text("Cancelled")).isTrue();
        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
    }

    @Test
    void wrongEmailShowsNeutralMessage() {
        var reservation = book(today.plusDays(5));
        navigate(MyReservationView.class);

        lookUp(reservation.getReservationNumber(), "wrong@example.com");

        assertThat(text(ReservationService.NOT_FOUND)).isTrue();
        assertThat(text("Test My Double")).isFalse();
    }

    @Test
    void noCancelButtonOnArrivalDay() {
        var reservation = book(today);
        navigate(MyReservationView.class);

        lookUp(reservation.getReservationNumber(), "my@example.com");

        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
        assertThat(text("Online cancellation is only possible until the day before arrival. Please call reception."))
                .isTrue();
    }

    private void lookUp(String number, String email) {
        test($(TextField.class).withLabel("Reservation number").single()).setValue(number);
        test($(EmailField.class).withLabel("Email").single()).setValue(email);
        test($(Button.class).withText("Show reservation").single()).click();
    }

    private boolean text(String text) {
        return $(Paragraph.class).withText(text).exists();
    }

    private ReservationRecord book(LocalDate arrival) {
        return reservationService.book(new BookingRequest(roomId, arrival, arrival.plusDays(2), 2, "Test My Guest",
                "my@example.com", "+41 79 000 00 00", BookingSource.ONLINE));
    }
}
