package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: each booking must commit in its own transaction to race for the room lock (ADR-001).
 */
@SpringBootTest
class ReservationConcurrencyTest {

    private static final int PARALLEL_BOOKINGS = 8;
    // far in the future so no other test data overlaps
    private static final StayPeriod STAY = new StayPeriod(LocalDate.now().plusDays(300), LocalDate.now().plusDays(302));

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DSLContext dsl;

    @AfterEach
    void deleteReservations() {
        dsl.deleteFrom(RESERVATION).where(RESERVATION.ARRIVAL.eq(STAY.arrival())).execute();
    }

    @Test
    void exactlyOneOfParallelBookingsForTheSameRoomAndNightsSucceeds() throws Exception {
        long roomId = dsl.select(ROOM.ID).from(ROOM).where(ROOM.NUMBER.eq("105")).fetchSingle(ROOM.ID);
        var start = new CountDownLatch(1);
        var results = new ArrayList<Future<String>>();

        try (var executor = Executors.newFixedThreadPool(PARALLEL_BOOKINGS)) {
            for (int i = 0; i < PARALLEL_BOOKINGS; i++) {
                var guest = new Guest("Guest " + i, "guest" + i + "@example.com", "000");
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        return reservationService.createReservation(roomId, STAY, 2, guest);
                    } catch (RoomNotAvailableException e) {
                        return null;
                    }
                }));
            }
            start.countDown();
        }

        int succeeded = 0;
        for (var result : results) {
            if (result.get() != null) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(dsl.fetchCount(RESERVATION, RESERVATION.ROOM_ID.eq(roomId)
                .and(RESERVATION.ARRIVAL.eq(STAY.arrival())))).isEqualTo(1);
    }
}
