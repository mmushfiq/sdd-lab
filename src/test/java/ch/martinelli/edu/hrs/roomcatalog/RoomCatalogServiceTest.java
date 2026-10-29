package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationTestData;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static ch.martinelli.edu.hrs.reservation.ReservationStatus.CANCELLED;
import static ch.martinelli.edu.hrs.reservation.ReservationStatus.CONFIRMED;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RoomCatalogServiceTest {

    @Autowired
    private RoomCatalogService roomCatalogService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    private ReservationTestData data;

    @BeforeEach
    void setUp() {
        data = new ReservationTestData(dsl);
    }

    @Test
    void bookableRoomsMatchMinimumCapacity() {
        assertThat(roomCatalogService.findBookableRooms(3)).extracting(RoomListItem::number)
                .containsExactly("109", "110");
    }

    @Test
    void bookableRoomsExcludeOutOfServiceRooms() {
        setOutOfService("104");

        assertThat(roomCatalogService.findBookableRooms(2)).extracting(RoomListItem::number)
                .containsExactly("105", "106", "107", "108", "109", "110");
    }

    @Test
    void noBookableRoomsWhenAllAreOutOfService() {
        roomCatalogService.findRooms().forEach(r -> setOutOfService(r.number()));

        assertThat(roomCatalogService.findBookableRooms(1)).isEmpty();
    }

    // 002-reservation / 006 protect rooms with reservations

    @Test
    void roomWithAnyReservationCannotBeDeleted() {
        data.insert("PASTPAST", "101", -5, -3, CANCELLED);

        assertThatThrownBy(() -> roomCatalogService.deleteRoom(data.roomId("101")))
                .isInstanceOf(RoomHasReservationsException.class)
                .hasMessage("Room has reservations and cannot be deleted; set it out of service instead");
        assertThat(roomCatalogService.findRooms()).extracting(RoomListItem::number).contains("101");
    }

    @Test
    void roomWithFutureConfirmedReservationsCannotGoOutOfServiceAndMessageListsThem() {
        var first = book("104", 5, 7);
        var second = book("104", 10, 12);

        assertThatThrownBy(() -> setOutOfService("104"))
                .isInstanceOf(RoomHasFutureReservationsException.class)
                .hasMessage("Room has future reservations and cannot be set out of service: %s, %s"
                        .formatted(first, second));
        assertThat(room("104").outOfService()).isFalse();
    }

    @Test
    void roomWithOnlyCancelledPastOrTodayDepartingReservationsCanGoOutOfService() {
        data.cancel(book("104", 5, 7));
        data.insert("PASTPAST", "104", -5, -3, CONFIRMED);
        data.insert("LASTDAY1", "104", -2, 0, CONFIRMED);

        setOutOfService("104");

        assertThat(room("104").outOfService()).isTrue();
    }

    private String book(String roomNumber, int arrivalInDays, int departureInDays) {
        return reservationService.createReservation(data.roomId(roomNumber), stay(arrivalInDays, departureInDays), 1,
                new Guest("Anna Müller", "anna@example.com", "000"));
    }

    private RoomListItem room(String number) {
        return roomCatalogService.findRooms().stream().filter(r -> r.number().equals(number)).findFirst()
                .orElseThrow();
    }

    private void setOutOfService(String number) {
        var room = roomCatalogService.findRooms().stream().filter(r -> r.number().equals(number)).findFirst()
                .orElseThrow();
        roomCatalogService.saveRoom(new RoomRecord(room.id(), room.number(), room.roomTypeId(), true));
    }
}
