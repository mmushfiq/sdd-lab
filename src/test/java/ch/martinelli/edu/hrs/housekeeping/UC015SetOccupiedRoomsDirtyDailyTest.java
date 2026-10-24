package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.usecase.UseCase;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-015 "Set Occupied Rooms Dirty Daily". It runs the daily cleaning run on the rooms of all test
 * data, e.g. the Cabin rooms 701 to 706 (V109) and the Pod rooms C01 to C08 (V113).
 */
@SpringBootTest
class UC015SetOccupiedRoomsDirtyDailyTest {

    @TestBean
    Clock clock;

    @Autowired
    DailyCleaningRun dailyCleaningRun;

    @Autowired
    DSLContext ctx;

    private Result<RoomRecord> originalRooms;
    private Result<ReservationRecord> originalReservations;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-05-31T22:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void rememberTestData() {
        originalRooms = ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch();
        originalReservations = ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch();
    }

    @AfterEach
    void restoreTestData() {
        originalRooms.forEach(original -> ctx.update(ROOM)
                .set(ROOM.CLEANING_STATUS, original.getCleaningStatus())
                .where(ROOM.ID.eq(original.getId()))
                .execute());
        originalReservations.forEach(original -> {
            original.changed(true);
            original.changed(RESERVATION.ID, false);
            ctx.update(RESERVATION).set(original).where(RESERVATION.ID.eq(original.getId())).execute();
        });
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-015", businessRules = {"BR-002"})
    void every_occupied_room_is_set_to_dirty() {
        // Step 2: the rooms assigned to a Checked In reservation, e.g. the clean rooms 706 and 901
        List<Long> occupiedRooms = occupiedRoomIds();
        assertThat(occupiedRooms).contains(2106L, 2201L);
        assertThat(cleaningStatus(2106L)).isEqualTo("CLEAN");

        // Step 1
        int changed = dailyCleaningRun.run();

        // Step 3: every occupied room is dirty and on today's cleaning list
        assertThat(changed).isEqualTo(occupiedRooms.size());
        assertThat(occupiedRooms).allSatisfy(roomId -> assertThat(cleaningStatus(roomId)).isEqualTo("DIRTY"));
        assertThat(cleaningStatus(2106L)).isEqualTo("DIRTY");
    }

    @Test
    @UseCase(id = "UC-015", businessRules = {"BR-002"})
    void free_and_blocked_rooms_keep_their_cleaning_status() {
        Map<Long, String> before = cleaningStatusOfRoomsNotOccupied();

        dailyCleaningRun.run();

        // Free clean room 701, free dirty room 703, blocked free room A01, blocked dirty room C05
        assertThat(cleaningStatusOfRoomsNotOccupied()).isEqualTo(before);
        assertThat(cleaningStatus(2101L)).isEqualTo("CLEAN");
        assertThat(cleaningStatus(2103L)).isEqualTo("DIRTY");
        assertThat(cleaningStatus(2301L)).isEqualTo("CLEAN");
    }

    @Test
    @UseCase(id = "UC-015", businessRules = {"BR-002"})
    void already_dirty_occupied_rooms_stay_dirty_and_no_reservation_changes() {
        // Room 902 is occupied and already dirty
        assertThat(cleaningStatus(2202L)).isEqualTo("DIRTY");
        Result<ReservationRecord> reservationsBefore = ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch();

        dailyCleaningRun.run();

        assertThat(cleaningStatus(2202L)).isEqualTo("DIRTY");
        // Failure postcondition: no room is set to clean, no reservation is changed
        originalRooms.stream().filter(room -> room.getCleaningStatus().equals("DIRTY"))
                .forEach(room -> assertThat(cleaningStatus(room.getId())).isEqualTo("DIRTY"));
        assertThat(ctx.selectFrom(RESERVATION).orderBy(RESERVATION.ID).fetch()).isEqualTo(reservationsBefore);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: No occupied room
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-015", scenario = "A1: No Occupied Room")
    void no_cleaning_status_changes_when_no_room_is_occupied() {
        // Every checked-in guest has left
        ctx.update(RESERVATION)
                .set(RESERVATION.STATUS, "CHECKED_OUT")
                .set(RESERVATION.CHECKED_OUT_AT, LocalDateTime.of(2030, 5, 31, 23, 0))
                .where(RESERVATION.STATUS.eq("CHECKED_IN"))
                .execute();
        Result<RoomRecord> roomsBefore = ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch();

        int changed = dailyCleaningRun.run();

        assertThat(changed).isZero();
        assertThat(ctx.selectFrom(ROOM).orderBy(ROOM.ID).fetch()).isEqualTo(roomsBefore);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // BR-001, A2: Only at 00:00
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-015", scenario = "A2: Run Missed at 00:00", businessRules = {"BR-001"})
    void run_is_scheduled_only_at_midnight() throws NoSuchMethodException {
        Scheduled scheduled = DailyCleaningRun.class.getMethod("run").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("0 0 0 * * *");
        // A run missed at 00:00 is not made up during the day: the next run is at 00:00 of the following day
        CronExpression cron = CronExpression.parse(scheduled.cron());
        assertThat(cron.next(LocalDateTime.of(2030, 6, 1, 0, 0, 1))).isEqualTo(LocalDateTime.of(2030, 6, 2, 0, 0));
        assertThat(cron.next(LocalDateTime.of(2030, 6, 1, 9, 30))).isEqualTo(LocalDateTime.of(2030, 6, 2, 0, 0));
        // An empty fixedDelay/fixedRate/initialDelay means there is no other trigger
        assertThat(scheduled.fixedDelay()).isNegative();
        assertThat(scheduled.fixedRate()).isNegative();
        assertThat(scheduled.initialDelay()).isNegative();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private List<Long> occupiedRoomIds() {
        return ctx.selectDistinct(RESERVATION.ROOM_ID)
                .from(RESERVATION)
                .where(RESERVATION.STATUS.eq("CHECKED_IN"))
                .fetch(RESERVATION.ROOM_ID);
    }

    private Map<Long, String> cleaningStatusOfRoomsNotOccupied() {
        List<Long> occupied = occupiedRoomIds();
        return ctx.selectFrom(ROOM).fetch().stream()
                .filter(room -> !occupied.contains(room.getId()))
                .collect(Collectors.toMap(RoomRecord::getId, RoomRecord::getCleaningStatus));
    }

    private String cleaningStatus(Long roomId) {
        return ctx.select(ROOM.CLEANING_STATUS).from(ROOM).where(ROOM.ID.eq(roomId)).fetchSingle(ROOM.CLEANING_STATUS);
    }
}
