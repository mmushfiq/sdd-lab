package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.reservation.ReservationTestData;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GuestReservationViewTest extends HrsBrowserlessTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private ReservationService reservationService;

    private ReservationTestData data;

    @BeforeEach
    void navigateToView() {
        data = new ReservationTestData(dsl);
        navigate(GuestReservationView.class);
    }

    @Test
    void hasNoLinksToStaffViews() {
        assertThat($view(RouterLink.class).all()).extracting(RouterLink::getHref).containsExactly("booking");
    }

    @Test
    void showsReservationForNumberAndEmailInAnyCase() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);

        lookup(" abcdefg2 ", "TEST@example.com");

        var text = viewText();
        assertThat(text).contains("ABCDEFG2", "Double", "CONFIRMED", "100.00");
        assertThat(text).doesNotContain("104");
    }

    @Test
    void showsGenericMessageForWrongEmail() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);

        lookup("ABCDEFG2", "other@example.com");

        assertThat(viewText()).contains(GuestReservationView.NOT_FOUND);
    }

    @Test
    void showsSameGenericMessageForWrongNumber() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);

        lookup("ZZZZZZZ2", "test@example.com");

        assertThat(viewText()).contains(GuestReservationView.NOT_FOUND);
    }

    @Test
    void showsSameGenericMessageForAnonymizedReservation() {
        data.insert("ABCDEFG2", "104", -10, -8, ReservationStatus.CONFIRMED);
        reservationService.anonymizeGuest("ABCDEFG2");

        lookup("ABCDEFG2", "test@example.com");

        assertThat(viewText()).contains(GuestReservationView.NOT_FOUND);
    }

    @Test
    void requiresBothFields() {
        lookup("", "");

        assertThat(field("Booking number").getErrorMessage()).isEqualTo("Booking number is required");
        assertThat(field("Email").getErrorMessage()).isEqualTo("Email is required");
    }

    @Test
    void cancelsAfterConfirmation() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);
        lookup("ABCDEFG2", "test@example.com");

        test($view(Button.class).withText("Cancel reservation").single()).click();
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(status("ABCDEFG2")).isEqualTo("CANCELLED");
        assertThat(test($(Notification.class).last()).getText()).isEqualTo("Reservation cancelled");
        assertThat($view(Button.class).withText("Cancel reservation").exists()).isFalse();
        assertThat(viewText()).contains("CANCELLED");
    }

    @Test
    void choosingNoChangesNothing() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);
        lookup("ABCDEFG2", "test@example.com");

        test($view(Button.class).withText("Cancel reservation").single()).click();
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(status("ABCDEFG2")).isEqualTo("CONFIRMED");
    }

    @Test
    void offersNoCancelActionAfterTheDeadline() {
        // arrival today: the deadline was 14:00 yesterday
        data.insert("ABCDEFG2", "104", 0, 2, ReservationStatus.CONFIRMED);

        lookup("ABCDEFG2", "test@example.com");

        assertThat($view(Button.class).withText("Cancel reservation").exists()).isFalse();
        assertThat(viewText()).contains("Online cancellation is no longer possible. Please call the hotel.");
    }

    @Test
    void offersNoCancelActionForCancelledReservation() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CANCELLED);

        lookup("ABCDEFG2", "test@example.com");

        assertThat($view(Button.class).withText("Cancel reservation").exists()).isFalse();
        assertThat(viewText()).contains("CANCELLED").doesNotContain("Online cancellation is no longer possible");
    }

    @Test
    void rejectsConfirmAfterTheDeadlinePassedWhilePageWasOpen() {
        data.insert("ABCDEFG2", "104", 10, 12, ReservationStatus.CONFIRMED);
        lookup("ABCDEFG2", "test@example.com");
        test($view(Button.class).withText("Cancel reservation").single()).click();
        // the stay is moved so that the deadline is now over, as if time had passed
        dsl.update(RESERVATION).set(RESERVATION.ARRIVAL, ReservationTestData.day(0))
                .set(RESERVATION.DEPARTURE, ReservationTestData.day(2))
                .where(RESERVATION.BOOKING_NUMBER.eq("ABCDEFG2")).execute();

        test($(ConfirmDialog.class).single()).confirm();

        assertThat(test($(Notification.class).last()).getText())
                .isEqualTo("Online cancellation is no longer possible. Please call the hotel.");
        assertThat(status("ABCDEFG2")).isEqualTo("CONFIRMED");
    }

    private void lookup(String bookingNumber, String email) {
        test(field("Booking number")).setValue(bookingNumber);
        test(field("Email")).setValue(email);
        test($view(Button.class).withText("Search").single()).click();
    }

    private TextField field(String label) {
        return $view(TextField.class).withLabel(label).single();
    }

    private String viewText() {
        return $view(com.vaadin.flow.component.html.Div.class).all().stream()
                .map(d -> d.getElement().getTextRecursively()).collect(java.util.stream.Collectors.joining(" "));
    }

    private String status(String bookingNumber) {
        return dsl.select(RESERVATION.STATUS).from(RESERVATION).where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber))
                .fetchSingle(RESERVATION.STATUS);
    }
}
