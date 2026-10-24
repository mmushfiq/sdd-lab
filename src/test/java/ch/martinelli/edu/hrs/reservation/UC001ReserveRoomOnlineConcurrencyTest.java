package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.email.EmailService;
import ch.martinelli.edu.hrs.email.RecordingEmailService;
import ch.martinelli.edu.hrs.usecase.UseCase;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-001 "Reserve Room Online": guests confirming the last room at the same time (BR-005, NFR-004).
 * Several guests cannot confirm at the same moment through one browserless UI, so this test confirms through the
 * service from parallel threads. Test data: V100__uc001_test_data.sql; today is 1 June 2030.
 */
@SpringBootTest
class UC001ReserveRoomOnlineConcurrencyTest {

    private static final int GUESTS = 10;
    private static final long SINGLE_ROOM = 1001L;
    private static final long FIRST_TEST_DATA_ID = 1001;

    @TestBean
    Clock clock;

    @TestBean
    EmailService emailService;

    @Autowired
    OnlineReservationService reservationService;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    static EmailService emailService() {
        return new RecordingEmailService();
    }

    @AfterEach
    void removeCreatedData() {
        ctx.deleteFrom(RESERVATION).where(RESERVATION.ID.lt(FIRST_TEST_DATA_ID)).execute();
        ctx.deleteFrom(GUEST).where(GUEST.ID.lt(FIRST_TEST_DATA_ID)).execute();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A5: Room Type No Longer Available", businessRules = {"BR-005"})
    void exactly_one_of_ten_simultaneous_reservations_of_the_last_room_succeeds() throws Exception {
        // The only Single Room is free for the night of 20 August 2030
        LocalDate arrival = LocalDate.of(2030, 8, 20);
        LocalDate departure = LocalDate.of(2030, 8, 21);
        assertThat(reservationService.searchAvailableRoomTypes(arrival, departure, 1))
                .extracting(AvailableRoomType::name)
                .contains("Single Room");

        CountDownLatch start = new CountDownLatch(1);
        List<Future<ReservationConfirmation>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(GUESTS)) {
            for (int i = 0; i < GUESTS; i++) {
                OnlineReservationRequest request = new OnlineReservationRequest(arrival, departure, 1, SINGLE_ROOM,
                        new GuestDetails("Guest", "Number " + i, "guest" + i + "@example.com", "+41 79 000 00 0" + i));
                results.add(executor.submit(() -> {
                    start.await();
                    return reservationService.reserve(request);
                }));
            }
            start.countDown();

            int succeeded = 0;
            int refused = 0;
            for (Future<ReservationConfirmation> result : results) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                    succeeded++;
                } catch (java.util.concurrent.ExecutionException e) {
                    // A5: the others are refused because the room type is no longer available
                    assertThat(e.getCause()).isInstanceOf(RoomTypeNoLongerAvailableException.class);
                    refused++;
                }
            }

            // NFR-004: exactly one succeeds and the others are refused
            assertThat(succeeded).isEqualTo(1);
            assertThat(refused).isEqualTo(GUESTS - 1);
        }

        // Failure postconditions: no overbooking, and no guest recorded for a refused reservation
        assertThat(ctx.fetchCount(RESERVATION, RESERVATION.ROOM_TYPE_ID.eq(SINGLE_ROOM)
                .and(RESERVATION.ARRIVAL_DATE.eq(arrival)))).isEqualTo(1);
        assertThat(ctx.fetchCount(GUEST, GUEST.ID.lt(FIRST_TEST_DATA_ID))).isEqualTo(1);
    }
}
