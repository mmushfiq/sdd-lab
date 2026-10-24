package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;

/**
 * Counts the occupied and available room nights of every day of a period (UC-019 step 4). Only reads data.
 */
@Repository
public class OccupancyReportRepository {

    private final DSLContext ctx;

    public OccupancyReportRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The occupied and available room nights of every day from {@code firstDate} to {@code lastDate}; the day
     * stands for the night that starts on it.
     */
    public List<DailyOccupancy> findDailyOccupancy(LocalDate firstDate, LocalDate lastDate) {
        // UC-019 BR-003: Every room that is currently active counts, unless it is blocked that night.
        int activeRooms = ctx.fetchCount(ROOM, ROOM.ACTIVE.isTrue());
        List<DateRange> blocks = ctx.select(ROOM_BLOCK.ROOM_ID, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE)
                .from(ROOM_BLOCK)
                .join(ROOM).on(ROOM.ID.eq(ROOM_BLOCK.ROOM_ID))
                .where(ROOM.ACTIVE.isTrue())
                .and(ROOM_BLOCK.START_DATE.le(lastDate))
                .and(ROOM_BLOCK.END_DATE.ge(firstDate))
                .fetch(Records.mapping(DateRange::new));
        // UC-019 BR-002: Every Checked In or Checked Out reservation counts from the arrival date to the night
        // before the departure date.
        List<DateRange> stays = ctx.select(RESERVATION.ID, RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE)
                .from(RESERVATION)
                .where(RESERVATION.STATUS.in(ReservationStatus.CHECKED_IN.name(),
                        ReservationStatus.CHECKED_OUT.name()))
                .and(RESERVATION.ARRIVAL_DATE.le(lastDate))
                .and(RESERVATION.DEPARTURE_DATE.gt(firstDate))
                .fetch(Records.mapping(DateRange::new));

        List<DailyOccupancy> days = new ArrayList<>();
        for (LocalDate day = firstDate; !day.isAfter(lastDate); day = day.plusDays(1)) {
            LocalDate night = day;
            // UC-019 BR-003: A blocked room is not available on the nights from the first to the last blocked date.
            long blockedRooms = blocks.stream()
                    .filter(block -> !night.isBefore(block.from()) && !night.isAfter(block.to()))
                    .map(DateRange::key)
                    .distinct()
                    .count();
            // UC-019 BR-002: The stay includes the nights from the arrival date to the night before departure.
            long occupied = stays.stream()
                    .filter(stay -> !night.isBefore(stay.from()) && night.isBefore(stay.to()))
                    .count();
            days.add(new DailyOccupancy(day, new OccupancyFigures(occupied, activeRooms - blockedRooms)));
        }
        return days;
    }

    private record DateRange(Long key, LocalDate from, LocalDate to) {
    }
}
