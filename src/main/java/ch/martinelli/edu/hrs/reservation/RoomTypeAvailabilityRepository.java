package ch.martinelli.edu.hrs.reservation;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.db.Tables.*;
import static org.jooq.impl.DSL.noCondition;

/**
 * Availability of room types for a date range (UC-001 step 3 and step 8).
 */
@Repository
public class RoomTypeAvailabilityRepository {

    private final DSLContext ctx;

    public RoomTypeAvailabilityRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns the room types available for every night from the arrival date to the night before the departure
     * date that fit the number of guests, cheapest first.
     */
    public List<RoomTypeOffer> findAvailableRoomTypes(LocalDate arrivalDate, LocalDate departureDate,
                                                      int numberOfGuests) {
        return findAvailable(arrivalDate, departureDate, numberOfGuests, null);
    }

    /**
     * Returns the given room type when it is available for every night of the date range and fits the number of
     * guests. Call it after {@link #lockRoomType(Long)} in the same transaction.
     */
    public Optional<RoomTypeOffer> findAvailableRoomType(Long roomTypeId, LocalDate arrivalDate,
                                                         LocalDate departureDate, int numberOfGuests) {
        return findAvailable(arrivalDate, departureDate, numberOfGuests, roomTypeId).stream().findFirst();
    }

    /**
     * Locks the room type row until the end of the current transaction, so that concurrent reservations of the same
     * room type check and record one after the other.
     */
    // UC-001 BR-005: The availability check and the recording of the reservation are one indivisible action.
    public void lockRoomType(Long roomTypeId) {
        ctx.select(ROOM_TYPE.ID)
                .from(ROOM_TYPE)
                .where(ROOM_TYPE.ID.eq(roomTypeId))
                .forUpdate()
                .fetchOptional();
    }

    private List<RoomTypeOffer> findAvailable(LocalDate arrivalDate, LocalDate departureDate, int numberOfGuests,
                                              Long roomTypeId) {
        LocalDate lastNight = departureDate.minusDays(1);

        List<RoomTypeOffer> candidates = ctx
                .select(ROOM_TYPE.ID, ROOM_TYPE.NAME, ROOM_TYPE.DESCRIPTION, ROOM_TYPE.MAX_GUESTS,
                        ROOM_TYPE.NIGHTLY_RATE)
                .from(ROOM_TYPE)
                // UC-001 BR-002: Only active room types are offered.
                .where(ROOM_TYPE.ACTIVE.isTrue())
                // UC-001 BR-003: Room types whose maximum number of guests is lower than the request are not offered.
                .and(ROOM_TYPE.MAX_GUESTS.ge(numberOfGuests))
                .and(roomTypeId == null ? noCondition() : ROOM_TYPE.ID.eq(roomTypeId))
                .orderBy(ROOM_TYPE.NIGHTLY_RATE, ROOM_TYPE.NAME)
                .fetch(Records.mapping(RoomTypeOffer::new));
        if (candidates.isEmpty()) {
            return List.of();
        }
        Set<Long> roomTypeIds = candidates.stream().map(RoomTypeOffer::roomTypeId).collect(Collectors.toSet());

        // UC-001 BR-002: Only active rooms count for availability.
        Map<Long, Long> roomTypeIdByRoomId = ctx
                .select(ROOM.ID, ROOM.ROOM_TYPE_ID)
                .from(ROOM)
                .where(ROOM.ACTIVE.isTrue())
                .and(ROOM.ROOM_TYPE_ID.in(roomTypeIds))
                .fetchMap(ROOM.ID, ROOM.ROOM_TYPE_ID);

        List<DateRange> blocks = ctx
                .select(ROOM_BLOCK.ROOM_ID, ROOM_BLOCK.START_DATE, ROOM_BLOCK.END_DATE)
                .from(ROOM_BLOCK)
                .where(ROOM_BLOCK.ROOM_ID.in(roomTypeIdByRoomId.keySet()))
                .and(ROOM_BLOCK.START_DATE.le(lastNight))
                .and(ROOM_BLOCK.END_DATE.ge(arrivalDate))
                .fetch(Records.mapping(DateRange::new));

        List<DateRange> reservations = ctx
                .select(RESERVATION.ROOM_TYPE_ID, RESERVATION.ARRIVAL_DATE, RESERVATION.DEPARTURE_DATE)
                .from(RESERVATION)
                .where(RESERVATION.ROOM_TYPE_ID.in(roomTypeIds))
                // UC-001 BR-002: Confirmed and Checked In reservations take a room of their room type.
                .and(RESERVATION.STATUS.in(ReservationStatus.CONFIRMED.name(), ReservationStatus.CHECKED_IN.name()))
                .and(RESERVATION.ARRIVAL_DATE.lt(departureDate))
                .and(RESERVATION.DEPARTURE_DATE.gt(arrivalDate))
                .fetch(Records.mapping(DateRange::new));

        return candidates.stream()
                .filter(candidate -> isAvailableEveryNight(candidate.roomTypeId(), arrivalDate, departureDate,
                        roomTypeIdByRoomId, blocks, reservations))
                .toList();
    }

    // UC-001 BR-002: For every night, the active rooms not blocked that night outnumber the Confirmed and Checked In reservations.
    private static boolean isAvailableEveryNight(Long roomTypeId, LocalDate arrivalDate, LocalDate departureDate,
                                                 Map<Long, Long> roomTypeIdByRoomId, List<DateRange> blocks,
                                                 List<DateRange> reservations) {
        List<Long> roomIds = roomTypeIdByRoomId.entrySet().stream()
                .filter(entry -> entry.getValue().equals(roomTypeId))
                .map(Map.Entry::getKey)
                .toList();
        List<DateRange> blocksOfType = blocks.stream().filter(block -> roomIds.contains(block.key())).toList();
        List<DateRange> reservationsOfType = reservations.stream()
                .filter(reservation -> reservation.key().equals(roomTypeId))
                .toList();

        for (LocalDate night = arrivalDate; night.isBefore(departureDate); night = night.plusDays(1)) {
            LocalDate n = night;
            long blockedRooms = blocksOfType.stream()
                    // A block covers its start date to its end date, both included
                    .filter(block -> !n.isBefore(block.from()) && !n.isAfter(block.to()))
                    .map(DateRange::key)
                    .distinct()
                    .count();
            long freeRooms = roomIds.size() - blockedRooms;
            long reservedRooms = reservationsOfType.stream()
                    // A reservation covers its arrival date to the night before its departure date
                    .filter(reservation -> !n.isBefore(reservation.from()) && n.isBefore(reservation.to()))
                    .count();
            if (freeRooms <= reservedRooms) {
                return false;
            }
        }
        return true;
    }

    private record DateRange(Long key, LocalDate from, LocalDate to) {
    }
}
