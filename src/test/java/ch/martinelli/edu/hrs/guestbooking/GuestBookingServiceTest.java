package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationFilter;
import ch.martinelli.edu.hrs.reservation.ReservationNotModifiableException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.reservation.ReservationTestData;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GuestBookingServiceTest {

    private static final ZoneId ZURICH = ZoneId.of("Europe/Zurich");
    private static final Guest ANNA = new Guest("Anna Müller", "anna@example.com", "+41 79 123 45 67");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    private GuestBookingService service;
    private ReservationTestData data;

    @BeforeEach
    void setUp() {
        service = new GuestBookingService(reservationService, dsl, Clock.systemDefaultZone());
        data = new ReservationTestData(dsl);
    }

    // --- search ---

    @Test
    void groupsFreeRoomsByTypeWithoutRoomNumbers() {
        var result = service.searchByRoomType(stay(1, 3), 2);

        assertThat(result).extracting(RoomTypeAvailability::roomTypeName).containsExactly("Double", "Family");
        var doubles = result.getFirst();
        assertThat(doubles.freeRooms()).isEqualTo(5);
        assertThat(doubles.capacity()).isEqualTo(2);
        assertThat(doubles.nightlyRate()).isEqualByComparingTo("150.00");
        assertThat(doubles.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(result.get(1).freeRooms()).isEqualTo(2);
    }

    @Test
    void omitsRoomTypeWithoutFreeRoom() {
        bookOutAllDoubles();

        var result = service.searchByRoomType(stay(1, 3), 2);

        assertThat(result).extracting(RoomTypeAvailability::roomTypeName).containsExactly("Family");
    }

    @Test
    void countsFreeRoomsPerType() {
        data.insert("AAAAAAA2", "104", 1, 3, ReservationStatus.CONFIRMED);

        var result = service.searchByRoomType(stay(1, 3), 2);

        assertThat(result.getFirst().freeRooms()).isEqualTo(4);
    }

    // --- booking ---

    @Test
    void booksTheFreeRoomWithTheLowestNumber() {
        var reservation = service.bookRoomType("Double", stay(1, 3), 2, ANNA);

        assertThat(roomNumberOf(reservation.bookingNumber())).isEqualTo("104");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.totalPrice()).isEqualByComparingTo("300.00");
        assertThat(reservation.roomTypeName()).isEqualTo("Double");
        assertThat(roomNumberOf(service.bookRoomType("Double", stay(1, 3), 2, ANNA).bookingNumber()))
                .isEqualTo("105");
    }

    @Test
    void skipsRoomsOfTheTypeThatAreTaken() {
        data.insert("AAAAAAA3", "104", 1, 3, ReservationStatus.CONFIRMED);

        var reservation = service.bookRoomType("Double", stay(1, 3), 2, ANNA);

        assertThat(roomNumberOf(reservation.bookingNumber())).isEqualTo("105");
    }

    @Test
    void rejectsWhenNoRoomOfTheTypeIsLeftAndStoresNothing() {
        bookOutAllDoubles();
        var before = dsl.fetchCount(RESERVATION);

        assertThatThrownBy(() -> service.bookRoomType("Double", stay(1, 3), 2, ANNA))
                .isInstanceOf(RoomTypeNotAvailableException.class)
                .hasMessage("This room type is no longer available for these dates");
        assertThat(dsl.fetchCount(RESERVATION)).isEqualTo(before);
    }

    @Test
    void onlineBookingAppearsInTheReceptionList() {
        var reservation = service.bookRoomType("Double", stay(1, 3), 2, ANNA);

        var found = reservationService.findReservations(
                new ReservationFilter(reservation.bookingNumber(), null, null,
                        null, false));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().guestName()).isEqualTo("Anna Müller");
    }

    // --- lookup ---

    @Test
    void findsReservationIgnoringCaseAndSurroundingSpaces() {
        var booked = service.bookRoomType("Double", stay(1, 3), 2, ANNA);

        var found = service.findByBookingNumberAndEmail("  " + booked.bookingNumber().toLowerCase() + " ",
                " ANNA@Example.com ");

        assertThat(found).isPresent();
        assertThat(found.get().roomTypeName()).isEqualTo("Double");
        assertThat(found.get().numberOfGuests()).isEqualTo(2);
    }

    @Test
    void findsNothingForWrongNumberOrWrongEmail() {
        var booked = service.bookRoomType("Double", stay(1, 3), 2, ANNA);

        assertThat(service.findByBookingNumberAndEmail(booked.bookingNumber(), "other@example.com")).isEmpty();
        assertThat(service.findByBookingNumberAndEmail("ZZZZZZZZ", "anna@example.com")).isEmpty();
    }

    @Test
    void anonymizedReservationIsNotFoundEvenWithTheOldEmailOrTheReplacement() {
        var booked = service.bookRoomType("Double", stay(1, 3), 2, ANNA);
        reservationService.cancelReservation(booked.bookingNumber());
        reservationService.anonymizeGuest(booked.bookingNumber());

        assertThat(service.findByBookingNumberAndEmail(booked.bookingNumber(), "anna@example.com")).isEmpty();
        assertThat(service.findByBookingNumberAndEmail(booked.bookingNumber(), "Anonymized")).isEmpty();
    }

    // --- cancel ---

    @Test
    void cancelsOneSecondBeforeTheDeadlineAndFreesTheRoom() {
        var booked = service.bookRoomType("Double", stay(5, 7), 2, ANNA);
        var atService = serviceAt(booked, LocalDateTime.of(booked.arrival().minusDays(1), LocalTime.of(13, 59, 59)));

        atService.cancelByGuest(booked.bookingNumber(), "anna@example.com");

        assertThat(statusOf(booked.bookingNumber())).isEqualTo("CANCELLED");
        assertThat(roomNumberOf(service.bookRoomType("Double", stay(5, 7), 2, ANNA).bookingNumber()))
                .isEqualTo("104");
    }

    @Test
    void rejectsCancellationFromTheDeadlineOn() {
        var booked = service.bookRoomType("Double", stay(5, 7), 2, ANNA);
        var atService = serviceAt(booked, LocalDateTime.of(booked.arrival().minusDays(1), LocalTime.of(14, 0)));

        assertThat(atService.canCancel(booked)).isFalse();
        assertThatThrownBy(() -> atService.cancelByGuest(booked.bookingNumber(), "anna@example.com"))
                .isInstanceOf(CancellationDeadlinePassedException.class)
                .hasMessage("Online cancellation is no longer possible. Please call the hotel.");
        assertThat(statusOf(booked.bookingNumber())).isEqualTo("CONFIRMED");
    }

    @Test
    void deadlineIsInHotelTimeNotUtc() {
        var booked = service.bookRoomType("Double", stay(5, 7), 2, ANNA);
        // 13:30 Zurich is 12:30 or 11:30 UTC: still before the deadline whichever offset applies
        var before = serviceAt(booked, LocalDateTime.of(booked.arrival().minusDays(1), LocalTime.of(13, 30)));
        // 14:30 Zurich is already 13:30 or 12:30 UTC, which a UTC-based deadline would still allow
        var after = serviceAt(booked, LocalDateTime.of(booked.arrival().minusDays(1), LocalTime.of(14, 30)));

        assertThat(before.canCancel(booked)).isTrue();
        assertThat(after.canCancel(booked)).isFalse();
    }

    @Test
    void cancelledReservationCannotBeCancelledAgain() {
        var booked = service.bookRoomType("Double", stay(5, 7), 2, ANNA);
        service.cancelByGuest(booked.bookingNumber(), "anna@example.com");

        var cancelled = service.findByBookingNumberAndEmail(booked.bookingNumber(), "anna@example.com").orElseThrow();
        assertThat(service.canCancel(cancelled)).isFalse();
        assertThatThrownBy(() -> service.cancelByGuest(booked.bookingNumber(), "anna@example.com"))
                .isInstanceOf(ReservationNotModifiableException.class);
    }

    @Test
    void cannotCancelWithWrongEmail() {
        var booked = service.bookRoomType("Double", stay(5, 7), 2, ANNA);

        assertThatThrownBy(() -> service.cancelByGuest(booked.bookingNumber(), "other@example.com"))
                .isInstanceOf(ReservationNotModifiableException.class);
        assertThat(statusOf(booked.bookingNumber())).isEqualTo("CONFIRMED");
    }

    private void bookOutAllDoubles() {
        var rooms = new String[]{"104", "105", "106", "107", "108"};
        for (int i = 0; i < rooms.length; i++) {
            data.insert("BBBBBBB" + (i + 2), rooms[i], 1, 3, ReservationStatus.CONFIRMED);
        }
    }

    private GuestBookingService serviceAt(GuestReservation reservation, LocalDateTime hotelTime) {
        return new GuestBookingService(reservationService, dsl,
                Clock.fixed(hotelTime.atZone(ZURICH).toInstant(), ZURICH));
    }

    private String roomNumberOf(String bookingNumber) {
        return dsl.select(ROOM.NUMBER).from(RESERVATION).join(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber)).fetchSingle(ROOM.NUMBER);
    }

    private String statusOf(String bookingNumber) {
        return dsl.select(RESERVATION.STATUS).from(RESERVATION).where(RESERVATION.BOOKING_NUMBER.eq(bookingNumber))
                .fetchSingle(RESERVATION.STATUS);
    }
}
