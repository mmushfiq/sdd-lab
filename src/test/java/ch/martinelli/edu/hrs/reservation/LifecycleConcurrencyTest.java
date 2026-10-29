package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.roomcatalog.RoomCatalogService;
import ch.martinelli.edu.hrs.roomcatalog.RoomHasFutureReservationsException;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.day;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.stay;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: each operation commits in its own transaction and races for the row locks (ADR-001).
 * Dates far in the future keep the data apart from other tests.
 */
@SpringBootTest
class LifecycleConcurrencyTest {

    private static final Guest GUEST = new Guest("Guest", "guest@example.com", "000");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomCatalogService roomCatalogService;

    @Autowired
    private DSLContext dsl;

    private ReservationTestData data;

    @BeforeEach
    void setUp() {
        data = new ReservationTestData(dsl);
    }

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ARRIVAL.ge(day(400))).execute();
        data.setOutOfService("107", false);
    }

    @RepeatedTest(10)
    void changeAndNewBookingForTheSameNightsNeverBothSucceed() throws Exception {
        long room = data.roomId("106");
        var existing = reservationService.createReservation(room, stay(400, 402), 2, GUEST);

        var results = race(
                () -> reservationService.changeReservation(existing, room, stay(400, 404), 2, GUEST),
                () -> reservationService.createReservation(room, stay(402, 404), 2, GUEST));

        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(room)
                .and(RESERVATION.ARRIVAL.lt(day(404))).and(RESERVATION.DEPARTURE.gt(day(402))))).isEqualTo(1);
    }

    @RepeatedTest(10)
    void bookingAndOutOfServiceNeverBothSucceed() throws Exception {
        long room = data.roomId("107");
        var roomType = dsl.select(ROOM.ROOM_TYPE_ID).from(ROOM).where(ROOM.ID.eq(room)).fetchSingle(ROOM.ROOM_TYPE_ID);

        var results = race(
                () -> reservationService.createReservation(room, stay(400, 402), 2, GUEST),
                () -> roomCatalogService.saveRoom(new RoomRecord(room, "107", roomType, true)));

        boolean booked = dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(room)
                .and(RESERVATION.ARRIVAL.eq(day(400)))) == 1;
        boolean outOfService = dsl.select(ROOM.OUT_OF_SERVICE).from(ROOM).where(ROOM.ID.eq(room))
                .fetchSingle(ROOM.OUT_OF_SERVICE);
        assertThat(booked && outOfService).as("booked room set out of service").isFalse();
        assertThat(booked || outOfService).isTrue();
    }

    /**
     * Starts both operations at the same moment; true means the operation succeeded, false that it was rejected.
     */
    private Boolean[] race(Runnable first, Runnable second) throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(rejectedAsFalse(start, first));
            var b = executor.submit(rejectedAsFalse(start, second));
            start.countDown();
            return new Boolean[]{a.get(), b.get()};
        }
    }

    private static Callable<Boolean> rejectedAsFalse(CountDownLatch start, Runnable operation) {
        return () -> {
            start.await();
            try {
                operation.run();
                return true;
            } catch (RoomNotAvailableException | RoomHasFutureReservationsException e) {
                return false;
            }
        };
    }
}
