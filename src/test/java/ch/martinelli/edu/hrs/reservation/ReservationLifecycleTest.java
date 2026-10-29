package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.reservation.ReservationStatus.CANCELLED;
import static ch.martinelli.edu.hrs.reservation.ReservationStatus.CONFIRMED;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.day;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationLifecycleTest {

    private static final Guest GUEST = new Guest("Anna Müller", "anna@example.com", "+41 79 123 45 67");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    private ReservationTestData data;

    @BeforeEach
    void setUp() {
        data = new ReservationTestData(dsl);
    }

    // 004 change

    @Test
    void changingDatesRecalculatesPriceWithCurrentRateAndKeepsBookingNumber() {
        var bookingNumber = book("104", 5, 7, 2);
        data.setRate("Double", "160.00");

        reservationService.changeReservation(bookingNumber, data.roomId("104"), stay(10, 13), 2, GUEST);

        var changed = find(bookingNumber);
        assertThat(changed.bookingNumber()).isEqualTo(bookingNumber);
        assertThat(changed.period()).isEqualTo(stay(10, 13));
        assertThat(changed.totalPrice()).isEqualByComparingTo("480.00");
    }

    @Test
    void extendingDoesNotConflictWithItself() {
        var bookingNumber = book("101", 10, 12, 1);

        reservationService.changeReservation(bookingNumber, data.roomId("101"), stay(10, 13), 1, GUEST);

        assertThat(find(bookingNumber).departure()).isEqualTo(day(13));
    }

    @Test
    void changeRejectsOverlapWithAnotherReservation() {
        var bookingNumber = book("101", 10, 12, 1);
        book("101", 12, 14, 1);

        assertThatThrownBy(() -> reservationService.changeReservation(bookingNumber, data.roomId("101"),
                stay(10, 13), 1, GUEST))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessage("Room is not available for these dates");
        assertThat(find(bookingNumber).departure()).isEqualTo(day(12));
    }

    @Test
    void changeRejectsRoomThatIsTooSmall() {
        var bookingNumber = book("104", 5, 7, 2);

        assertThatThrownBy(() -> reservationService.changeReservation(bookingNumber, data.roomId("101"),
                stay(5, 7), 2, GUEST)).isInstanceOf(CapacityExceededException.class);
        assertThat(find(bookingNumber).roomNumber()).isEqualTo("104");
    }

    @Test
    void changeRejectsRoomOutOfService() {
        var bookingNumber = book("104", 5, 7, 2);
        data.setOutOfService("105", true);

        assertThatThrownBy(() -> reservationService.changeReservation(bookingNumber, data.roomId("105"),
                stay(5, 7), 2, GUEST)).isInstanceOf(RoomNotAvailableException.class);
        assertThat(find(bookingNumber).roomNumber()).isEqualTo("104");
    }

    @Test
    void changeIsNotPossibleForCancelledOrDepartedReservations() {
        var cancelled = book("104", 5, 7, 2);
        data.cancel(cancelled);
        var departed = data.insert("DEPARTED", "101", -3, -1, CONFIRMED);

        assertThatThrownBy(() -> reservationService.changeReservation(cancelled, data.roomId("104"), stay(5, 8), 2,
                GUEST)).isInstanceOf(ReservationNotModifiableException.class);
        assertThatThrownBy(() -> reservationService.changeReservation(departed, data.roomId("101"), stay(1, 2), 1,
                GUEST)).isInstanceOf(ReservationNotModifiableException.class);
    }

    @Test
    void changingOnlyGuestDataKeepsThePrice() {
        var bookingNumber = book("104", 5, 7, 2);
        data.setRate("Double", "999.00");

        reservationService.changeReservation(bookingNumber, data.roomId("104"), stay(5, 7), 2,
                new Guest(GUEST.name(), GUEST.email(), "044 000 00 00"));

        var changed = find(bookingNumber);
        assertThat(changed.guestPhone()).isEqualTo("044 000 00 00");
        assertThat(changed.totalPrice()).isEqualByComparingTo("300.00");
    }

    @Test
    void stayingGuestMayMoveDepartureButNotArrivalIntoThePast() {
        var staying = data.insert("STAYING1", "101", -2, 1, CONFIRMED);

        reservationService.changeReservation(staying, data.roomId("101"), stay(-2, 3), 1, GUEST);
        assertThat(find(staying).departure()).isEqualTo(day(3));

        assertThatIllegalArgumentException().isThrownBy(() -> reservationService.changeReservation(staying,
                data.roomId("101"), stay(-1, 3), 1, GUEST));
    }

    @Test
    void reservationDepartingTodayCanStillBeExtended() {
        var departingToday = data.insert("LASTDAY1", "101", -2, 0, CONFIRMED);

        reservationService.changeReservation(departingToday, data.roomId("101"), stay(-2, 1), 1, GUEST);

        assertThat(find(departingToday).departure()).isEqualTo(day(1));
    }

    // 005 cancel

    @Test
    void cancelKeepsReservationAndFreesTheNights() {
        var bookingNumber = book("101", 5, 7, 1);

        reservationService.cancelReservation(bookingNumber);

        assertThat(find(bookingNumber).status()).isEqualTo(CANCELLED);
        assertThat(reservationService.searchAvailability(stay(5, 7), 1)).extracting(AvailableRoom::number)
                .contains("101");
    }

    @Test
    void cancelledReservationCannotBeCancelledOrChangedAgain() {
        var bookingNumber = book("101", 5, 7, 1);
        reservationService.cancelReservation(bookingNumber);

        assertThatThrownBy(() -> reservationService.cancelReservation(bookingNumber))
                .isInstanceOf(ReservationNotModifiableException.class);
        assertThat(find(bookingNumber).changeable()).isFalse();
    }

    @Test
    void receptionMayCancelDuringTheStay() {
        var staying = data.insert("STAYING2", "101", -1, 2, CONFIRMED);

        reservationService.cancelReservation(staying);

        assertThat(find(staying).status()).isEqualTo(CANCELLED);
    }

    // 007 anonymize

    @Test
    void anonymizeReplacesPersonalDataAndKeepsTheRest() {
        var bookingNumber = book("104", 5, 7, 2);
        reservationService.cancelReservation(bookingNumber);

        reservationService.anonymizeGuest(bookingNumber);

        var anonymized = find(bookingNumber);
        assertThat(anonymized.guest()).isEqualTo(new Guest("Anonymized", "Anonymized", "Anonymized"));
        assertThat(anonymized.anonymized()).isTrue();
        assertThat(anonymized.roomNumber()).isEqualTo("104");
        assertThat(anonymized.period()).isEqualTo(stay(5, 7));
        assertThat(anonymized.numberOfGuests()).isEqualTo(2);
        assertThat(anonymized.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(anonymized.status()).isEqualTo(CANCELLED);
    }

    @Test
    void departedConfirmedReservationCanBeAnonymized() {
        var departed = data.insert("DEPARTED", "101", -3, -1, CONFIRMED);

        reservationService.anonymizeGuest(departed);

        assertThat(dsl.fetchSingle(RESERVATION, RESERVATION.BOOKING_NUMBER.eq(departed)).getAnonymized()).isTrue();
    }

    @Test
    void confirmedReservationDepartingTodayOrLaterCannotBeAnonymized() {
        var departingToday = data.insert("LASTDAY2", "101", -2, 0, CONFIRMED);
        var future = book("104", 5, 7, 2);

        assertThatThrownBy(() -> reservationService.anonymizeGuest(departingToday))
                .isInstanceOf(ReservationNotModifiableException.class);
        assertThatThrownBy(() -> reservationService.anonymizeGuest(future))
                .isInstanceOf(ReservationNotModifiableException.class);
    }

    @Test
    void anonymizedReservationCannotBeAnonymizedOrChangedAgain() {
        var departed = data.insert("DEPARTED", "101", -3, -1, CONFIRMED);
        reservationService.anonymizeGuest(departed);

        assertThatThrownBy(() -> reservationService.anonymizeGuest(departed))
                .isInstanceOf(ReservationNotModifiableException.class);
        var item = find(departed);
        assertThat(item.anonymizable()).isFalse();
        assertThat(item.changeable()).isFalse();
    }

    private String book(String roomNumber, int arrivalInDays, int departureInDays, int guests) {
        return reservationService.createReservation(data.roomId(roomNumber), stay(arrivalInDays, departureInDays),
                guests, GUEST);
    }

    private ReservationListItem find(String bookingNumber) {
        return data.find(reservationService, bookingNumber);
    }
}
