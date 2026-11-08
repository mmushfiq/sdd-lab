package ch.martinelli.edu.hrs.room;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static ch.martinelli.edu.hrs.room.SeasonalRateServiceTest.rate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: both saves must run in their own transactions at the same time (003 research R5).
 */
@SpringBootTest
class SeasonalRateConcurrencyTest {

    @Autowired
    private SeasonalRateService seasonalRateService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private DSLContext dsl;

    @AfterEach
    void cleanUp() {
        // on delete cascade removes the seasonal rates
        dsl.deleteFrom(ROOM_TYPE).where(ROOM_TYPE.NAME.eq("Test Concurrent Season")).execute();
    }

    @Test
    void overlappingRatesSavedAtOnceSucceedOnce() throws Exception {
        var typeId = roomTypeService.save(roomType("Test Concurrent Season", 2, "100.00")).getId();
        var start = new CountDownLatch(1);
        Callable<String> save = () -> {
            start.await();
            try {
                seasonalRateService.save(rate(typeId, "Summer", "2027-07-01", "2027-08-31", "150"));
                return null;
            } catch (BusinessRuleException e) {
                return e.getMessage();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(save);
            Future<String> second = executor.submit(save);
            start.countDown();

            assertThat(new String[]{first.get(), second.get()})
                    .containsExactlyInAnyOrder(null,
                            "Overlaps the seasonal rate \"Summer\" (1 July 2027 – 31 August 2027).");
        }
        assertThat(dsl.fetchCount(SEASONAL_RATE, SEASONAL_RATE.ROOM_TYPE_ID.eq(typeId))).isEqualTo(1);
    }
}
