package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: both bookings must run in their own transactions at the same time (research R3).
 */
@SpringBootTest
class ReservationConcurrencyTest {

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

    private Long roomId;

    @AfterEach
    void cleanUp() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ROOM_ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM).where(ROOM.ID.eq(roomId)).execute();
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("Test Concurrent")).execute();
    }

    @Test
    void sameRoomAndNightsBookedTwiceAtOnceSucceedsOnce() throws Exception {
        var type = roomTypeService.save(roomType("Test Concurrent", 2, "100.00"));
        var room = new RoomRecord();
        room.setRoomNumber("T701");
        room.setRoomTypeId(type.getId());
        roomId = roomService.save(room).getId();
        var arrival = LocalDate.now(clock).plusDays(5);
        var request = new BookingRequest(roomId, arrival, arrival.plusDays(2), 2,
                "Test Guest", "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION);

        var start = new CountDownLatch(1);
        Callable<Boolean> booking = () -> {
            start.await();
            try {
                reservationService.book(request);
                return true;
            } catch (BusinessRuleException e) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(booking);
            Future<Boolean> second = executor.submit(booking);
            start.countDown();

            assertThat(first.get() ^ second.get()).as("exactly one booking succeeds").isTrue();
        }
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(roomId))).isEqualTo(1);
    }
}
