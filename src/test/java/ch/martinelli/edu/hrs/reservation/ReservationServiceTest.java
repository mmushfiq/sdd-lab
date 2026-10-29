package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    private static final Guest GUEST = new Guest("Anna Müller", "Anna.Mueller@example.com", "+41 79 123 45 67");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    // search

    @Test
    void searchListsFreeInServiceRoomsWithEnoughCapacityAndTotalPrice() {
        var rooms = reservationService.searchAvailability(stay(1, 3), 2);

        assertThat(rooms).extracting(AvailableRoom::number)
                .containsExactly("104", "105", "106", "107", "108", "109", "110");
        var first = rooms.getFirst();
        assertThat(first.roomTypeName()).isEqualTo("Double");
        assertThat(first.capacity()).isEqualTo(2);
        assertThat(first.nightlyRate()).isEqualByComparingTo("150.00");
        assertThat(first.totalPrice()).isEqualByComparingTo("300.00");
    }

    @Test
    void searchPricesOneNightAtTheNightlyRate() {
        var room101 = reservationService.searchAvailability(stay(1, 2), 1).getFirst();

        assertThat(room101.number()).isEqualTo("101");
        assertThat(room101.totalPrice()).isEqualByComparingTo("120.00");
    }

    @Test
    void searchExcludesRoomWithOverlappingConfirmedReservation() {
        book("101", stay(10, 12), 1);

        assertThat(numbers(reservationService.searchAvailability(stay(11, 13), 1))).doesNotContain("101");
    }

    @Test
    void searchAllowsBackToBackStays() {
        book("101", stay(10, 12), 1);

        assertThat(numbers(reservationService.searchAvailability(stay(12, 14), 1))).contains("101");
    }

    @Test
    void searchIgnoresCancelledReservations() {
        cancel(book("101", stay(10, 12), 1));

        assertThat(numbers(reservationService.searchAvailability(stay(10, 12), 1))).contains("101");
    }

    @Test
    void searchExcludesOutOfServiceRooms() {
        setOutOfService("101");

        assertThat(numbers(reservationService.searchAvailability(stay(1, 2), 1))).doesNotContain("101");
    }

    @Test
    void searchRejectsPastArrivalAndZeroGuests() {
        assertThatIllegalArgumentException().isThrownBy(() -> reservationService.searchAvailability(stay(-1, 2), 1));
        assertThatIllegalArgumentException().isThrownBy(() -> reservationService.searchAvailability(stay(1, 2), 0));
    }

    // create

    @Test
    void createStoresConfirmedReservationPricedByNights() {
        var bookingNumber = book("104", stay(1, 4), 2);

        assertThat(bookingNumber).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        var stored = dsl.fetchSingle(RESERVATION, RESERVATION.BOOKING_NUMBER.eq(bookingNumber));
        assertThat(stored.getStatus()).isEqualTo("CONFIRMED");
        assertThat(stored.getTotalPrice()).isEqualByComparingTo("450.00");
        assertThat(stored.getNumberOfGuests()).isEqualTo(2);
        assertThat(stored.getGuestName()).isEqualTo(GUEST.name());
        assertThat(stored.getGuestEmail()).isEqualTo("Anna.Mueller@example.com");
        assertThat(stored.getGuestPhone()).isEqualTo(GUEST.phone());
        assertThat(stored.getAnonymized()).isFalse();
    }

    @Test
    void createRejectsMoreGuestsThanCapacity() {
        assertThatThrownBy(() -> book("101", stay(1, 2), 2))
                .isInstanceOf(CapacityExceededException.class)
                .hasMessage("This room allows at most 1 guests");
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void createRejectsRoomBookedForOverlappingNightsSinceTheSearch() {
        book("101", stay(5, 7), 1);

        assertThatThrownBy(() -> book("101", stay(6, 8), 1))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessage("Room is no longer available for these dates");
        assertThat(dsl.fetchCount(RESERVATION)).isEqualTo(1);
    }

    @Test
    void createRejectsRoomSetOutOfServiceSinceTheSearch() {
        setOutOfService("101");

        assertThatThrownBy(() -> book("101", stay(1, 2), 1)).isInstanceOf(RoomNotAvailableException.class);
        assertThat(dsl.fetchCount(RESERVATION)).isZero();
    }

    @Test
    void totalPriceStaysWhenRateChangesLater() {
        var bookingNumber = book("101", stay(1, 3), 1);
        dsl.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("999.00"))
                .where(ROOM_TYPE.NAME.eq("Single")).execute();

        assertThat(find(new ReservationFilter(bookingNumber, null, null, null, false)).getFirst().totalPrice())
                .isEqualByComparingTo("240.00");
    }

    // find

    @Test
    void listShowsUpcomingConfirmedReservationsSortedByArrival() {
        var later = book("104", stay(5, 7), 2);
        var departingToday = insertDirectly("PASTPAST", "101", stay(-2, 0));
        insertDirectly("GONEGONE", "102", stay(-3, -1));
        cancel(book("105", stay(1, 3), 2));

        var list = find(new ReservationFilter(null, null, null, null, false));

        assertThat(list).extracting(ReservationListItem::bookingNumber).containsExactly(departingToday, later);
        var item = list.getLast();
        assertThat(item.guestName()).isEqualTo(GUEST.name());
        assertThat(item.roomNumber()).isEqualTo("104");
        assertThat(item.arrival()).isEqualTo(day(5));
        assertThat(item.departure()).isEqualTo(day(7));
        assertThat(item.numberOfGuests()).isEqualTo(2);
        assertThat(item.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(item.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void bookingNumberFindsExactlyThatReservationInAnyStatusIgnoringCase() {
        book("104", stay(5, 7), 2);
        var cancelled = book("105", stay(1, 3), 2);
        cancel(cancelled);

        var list = find(new ReservationFilter(" " + cancelled.toLowerCase() + " ", null, null, null, false));

        assertThat(list).extracting(ReservationListItem::bookingNumber).containsExactly(cancelled);
        assertThat(list.getFirst().status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void guestNameMatchesPartOfTheNameIgnoringCase() {
        var mueller = book("104", stay(1, 3), 2);
        reservationService.createReservation(roomId("105"), stay(1, 3), 2,
                new Guest("Hans Meier", "hans@example.com", "044 123 45 67"));

        assertThat(find(new ReservationFilter(null, "mül", null, null, false)))
                .extracting(ReservationListItem::bookingNumber).containsExactly(mueller);
    }

    @Test
    void dateRangeFindsStaysOverlappingTheRange() {
        book("101", stay(1, 3), 1);
        var overlapping = book("104", stay(5, 8), 2);
        var arrivingOnLastDay = book("105", stay(9, 10), 2);
        book("106", stay(10, 12), 2);

        assertThat(find(new ReservationFilter(null, null, day(6), day(9), false)))
                .extracting(ReservationListItem::bookingNumber).containsExactly(overlapping, arrivingOnLastDay);
    }

    @Test
    void showCancelledIncludesCancelledReservations() {
        var confirmed = book("104", stay(1, 3), 2);
        var cancelled = book("105", stay(2, 4), 2);
        cancel(cancelled);

        assertThat(find(new ReservationFilter(null, null, null, null, true)))
                .extracting(ReservationListItem::bookingNumber).containsExactly(confirmed, cancelled);
    }

    private String book(String roomNumber, StayPeriod period, int guests) {
        return reservationService.createReservation(roomId(roomNumber), period, guests, GUEST);
    }

    // bypasses the service so stays in the past can be set up
    private String insertDirectly(String bookingNumber, String roomNumber, StayPeriod period) {
        dsl.insertInto(RESERVATION)
                .set(RESERVATION.BOOKING_NUMBER, bookingNumber)
                .set(RESERVATION.ROOM_ID, roomId(roomNumber))
                .set(RESERVATION.ARRIVAL, period.arrival())
                .set(RESERVATION.DEPARTURE, period.departure())
                .set(RESERVATION.NUMBER_OF_GUESTS, 1)
                .set(RESERVATION.GUEST_NAME, "Past Guest")
                .set(RESERVATION.GUEST_EMAIL, "past@example.com")
                .set(RESERVATION.GUEST_PHONE, "000")
                .set(RESERVATION.TOTAL_PRICE, new BigDecimal("100.00"))
                .set(RESERVATION.STATUS, "CONFIRMED")
                .execute();
        return bookingNumber;
    }

    private void cancel(String bookingNumber) {
        dsl.update(RESERVATION).set(RESERVATION.STATUS, "CANCELLED")
                .where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber)).execute();
    }

    private void setOutOfService(String roomNumber) {
        dsl.update(ROOM).set(ROOM.OUT_OF_SERVICE, true).where(ROOM.NUMBER.eq(roomNumber)).execute();
    }

    private long roomId(String number) {
        return dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq(number)).fetchSingle(ROOM.ID);
    }

    private List<ReservationListItem> find(ReservationFilter filter) {
        return reservationService.findReservations(filter);
    }

    private static List<String> numbers(List<AvailableRoom> rooms) {
        return rooms.stream().map(AvailableRoom::number).toList();
    }

    private static StayPeriod stay(int arrivalInDays, int departureInDays) {
        return new StayPeriod(day(arrivalInDays), day(departureInDays));
    }

    private static LocalDate day(int inDays) {
        return LocalDate.now().plusDays(inDays);
    }
}
