package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.CancellationDeadline;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
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
    void noCancelButtonWithin48HoursOfCheckIn() {
        var arrival = today.plusDays(1);
        var reservation = book(arrival);
        navigate(MyReservationView.class);

        lookUp(reservation.getReservationNumber(), "my@example.com");

        assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
        assertThat($(Paragraph.class).withId("cancellation-deadline").single().getText())
                .isEqualTo(CancellationDeadline.of(arrival, 48).passedText());
    }

    @Test
    void showsTheExactLastCancellationTime() {
        var arrival = today.plusDays(5);
        var reservation = book(arrival);
        navigate(MyReservationView.class);

        lookUp(reservation.getReservationNumber(), "my@example.com");

        assertThat($(Paragraph.class).withId("cancellation-deadline").single().getText())
                .isEqualTo(CancellationDeadline.of(arrival, 48).text())
                .startsWith("Free online cancellation until ")
                .endsWith(", 15:00 (Zurich time)");
        assertThat($(Button.class).withText("Cancel reservation").exists()).isTrue();
    }

    @Test
    void noCancellationTimeWhenNotBooked() {
        var cancelled = book(today.plusDays(5));
        reservationService.cancel(cancelled.getId());
        var checkedIn = book(today);
        reservationService.checkIn(checkedIn.getId());
        navigate(MyReservationView.class);

        for (var number : new String[]{cancelled.getReservationNumber(), checkedIn.getReservationNumber()}) {
            lookUp(number, "my@example.com");

            assertThat($(Paragraph.class).withId("cancellation-deadline").exists()).isFalse();
            assertThat($(Button.class).withText("Cancel reservation").exists()).isFalse();
        }
    }

    @Test
    void deadlinePassingBeforeConfirmationIsRejected() {
        var arrival = today.plusDays(5);
        var reservation = book(arrival);
        navigate(MyReservationView.class);
        lookUp(reservation.getReservationNumber(), "my@example.com");
        test($(Button.class).withText("Cancel reservation").single()).click();

        // The deadline passes while the confirm dialog is open (contracts/ui-changes.md)
        dsl.update(RESERVATION).set(RESERVATION.CANCELLATION_HOURS_BEFORE, 200)
                .where(RESERVATION.ID.eq(reservation.getId())).execute();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo(CancellationDeadline.of(arrival, 200).passedText());
        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(reservation.getId())).getStatus())
                .isEqualTo("BOOKED");
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
