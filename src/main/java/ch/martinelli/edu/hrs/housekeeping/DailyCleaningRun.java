package ch.martinelli.edu.hrs.housekeeping;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.selectOne;

/**
 * UC-015 Set Occupied Rooms Dirty Daily: at the start of each day every occupied room is set to dirty, so that
 * staying guests get their room cleaned daily.
 */
@Component
public class DailyCleaningRun {

    /**
     * 00:00 every day. The cron runs in the default time zone of the server, which is also the zone of the clock
     * that decides "today" (hotel local time).
     */
    public static final String CRON = "0 0 0 * * *";

    private static final Logger log = LoggerFactory.getLogger(DailyCleaningRun.class);

    private final DSLContext ctx;

    public DailyCleaningRun(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Steps 1 to 3, A1.
     *
     * @return the number of rooms set to dirty
     */
    // UC-015 BR-001: Only at 00:00 hotel local time. A missed run is not made up later that day (A2): Spring does not
    // repeat a cron run that was missed while the application was not running.
    @Scheduled(cron = CRON)
    public int run() {
        int rooms = ctx.update(ROOM)
                .set(ROOM.CLEANING_STATUS, "DIRTY")
                // UC-015 BR-002: Only rooms assigned to a Checked In reservation; free and blocked rooms keep their
                // cleaning status, and no room is ever set to clean by the run.
                .where(exists(selectOne()
                        .from(RESERVATION)
                        .where(RESERVATION.ROOM_ID.eq(ROOM.ID))
                        .and(RESERVATION.STATUS.eq(ReservationStatus.CHECKED_IN.name()))))
                .execute();
        log.info("Daily cleaning run set {} occupied rooms to dirty", rooms);
        return rooms;
    }
}
