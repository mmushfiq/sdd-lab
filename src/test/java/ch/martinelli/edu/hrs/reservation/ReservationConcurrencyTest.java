package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario "Two bookings at the same moment". Not transactional: each thread commits its own transaction.
 */
@SpringBootTest
class ReservationConcurrencyTest {

    @Autowired
    ReservationService service;

    @Autowired
    DSLContext ctx;

    @AfterEach
    void deleteReservations() {
        ctx.deleteFrom(RESERVATION).execute();
    }

    @RepeatedTest(5)
    void only_one_of_two_simultaneous_bookings_is_saved() {
        long room104 = roomId(ctx, "104");
        CyclicBarrier start = new CyclicBarrier(2);
        List<CompletableFuture<String>> results = new ArrayList<>();
        for (Stay stay : List.of(new Stay(room104, MARCH_10, MARCH_12, 2), new Stay(room104, MARCH_11, MARCH_14, 2))) {
            results.add(CompletableFuture.supplyAsync(() -> {
                try {
                    start.await();
                    service.create(stay, new Guest("Anna Meier", "anna@example.com", null));
                    return "saved";
                } catch (ReservationException e) {
                    return e.getMessage();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }));
        }

        assertThat(results).extracting(CompletableFuture::join)
                .containsExactlyInAnyOrder("saved", "The room is no longer available for these dates");
        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(1);
    }
}
