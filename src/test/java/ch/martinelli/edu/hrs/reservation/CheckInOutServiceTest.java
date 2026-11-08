package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import ch.martinelli.edu.hrs.room.RoomTypeService;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.room.RoomTypeServiceTest.roomType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CheckInOutServiceTest {

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

    private LocalDate today;
    private RoomTypeRecord type;

    @BeforeEach
    void createType() {
        today = LocalDate.now(clock);
        type = roomTypeService.save(roomType("Test Front Desk", 2, "100.00"));
    }

    @Test
    void checkInBookedReservationArrivingTodayIntoCleanRoom() {
        var id = book(room("T701"), today, today.plusDays(2));

        reservationService.checkIn(id);

        assertThat(status(id)).isEqualTo(ReservationStatus.CHECKED_IN.name());
    }

    @Test
    void checkInToDirtyRoomIsRejected() {
        var roomId = room("T702");
        var id = book(roomId, today, today.plusDays(2));
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ID.eq(roomId)).execute();

        assertThatThrownBy(() -> reservationService.checkIn(id))
                .isInstanceOf(BusinessRuleException.class).hasMessage("The room is not clean yet.");
        assertThat(status(id)).isEqualTo(ReservationStatus.BOOKED.name());
    }

    @Test
    void checkInBeforeArrivalDateIsRejected() {
        var id = book(room("T703"), today.plusDays(1), today.plusDays(2));

        assertThatThrownBy(() -> reservationService.checkIn(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Check-in is only possible on the arrival date.");
    }

    @Test
    void checkInTwiceIsRejected() {
        var id = book(room("T704"), today, today.plusDays(2));
        reservationService.checkIn(id);

        assertThatThrownBy(() -> reservationService.checkIn(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only a booked reservation can be checked in.");
    }

    @Test
    void checkOutMarksRoomDirty() {
        var roomId = room("T705");
        var id = book(roomId, today, today.plusDays(2));
        reservationService.checkIn(id);

        reservationService.checkOut(id);

        assertThat(status(id)).isEqualTo(ReservationStatus.CHECKED_OUT.name());
        assertThat(dsl.fetchSingle(ROOM, ROOM.ID.eq(roomId)).getCleaningStatus()).isEqualTo("DIRTY");
    }

    @Test
    void checkOutOfBookedReservationIsRejected() {
        var id = book(room("T706"), today, today.plusDays(2));

        assertThatThrownBy(() -> reservationService.checkOut(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only a checked-in reservation can be checked out.");
    }

    @Test
    void arrivalsAndDeparturesOfToday() {
        var arriving = book(room("T707"), today, today.plusDays(1));
        book(room("T708"), today.plusDays(1), today.plusDays(2));
        var departing = checkedInSince(room("T709"), today.minusDays(2), today);

        assertThat(reservationService.findArrivals(today)).extracting(Stay::reservationId).contains(arriving)
                .doesNotContain(departing);
        assertThat(reservationService.findArrivals(today)).extracting(Stay::roomNumber).doesNotContain("T708");
        assertThat(reservationService.findDepartures(today)).extracting(Stay::reservationId)
                .containsExactly(departing);
    }

    @Test
    void overdueGuestIsListedForCheckOut() {
        var overdue = checkedInSince(room("T714"), today.minusDays(3), today.minusDays(1));

        assertThat(reservationService.findDepartures(today)).extracting(Stay::reservationId).contains(overdue);
    }

    @Test
    void overdueGuestKeepsRoomOccupied() {
        checkedInSince(room("T715"), today.minusDays(3), today.minusDays(1));

        assertThat(reservationService.roomOverview(today)).filteredOn(r -> r.roomNumber().equals("T715"))
                .extracting(RoomStatus::occupancy).containsExactly(Occupancy.OCCUPIED);
    }

    @Test
    void roomOverviewShowsOccupancyAndCleaningStatus() {
        room("T710");
        book(room("T711"), today, today.plusDays(2));
        checkedInSince(room("T712"), today.minusDays(1), today.plusDays(1));
        checkedInSince(room("T713"), today.minusDays(1), today);
        dsl.update(ROOM).set(ROOM.CLEANING_STATUS, "DIRTY").where(ROOM.ROOM_NUMBER.eq("T710")).execute();

        assertThat(reservationService.roomOverview(today))
                .filteredOn(r -> r.roomNumber().startsWith("T71"))
                .containsExactly(
                        new RoomStatus("T710", "Test Front Desk", Occupancy.FREE, "DIRTY"),
                        new RoomStatus("T711", "Test Front Desk", Occupancy.BOOKED, "CLEAN"),
                        new RoomStatus("T712", "Test Front Desk", Occupancy.OCCUPIED, "CLEAN"),
                        new RoomStatus("T713", "Test Front Desk", Occupancy.OCCUPIED, "CLEAN"));
    }

    private long room(String number) {
        var room = new RoomRecord();
        room.setRoomNumber(number);
        room.setRoomTypeId(type.getId());
        return roomService.save(room).getId();
    }

    private long book(long roomId, LocalDate arrival, LocalDate departure) {
        return reservationService.book(new BookingRequest(roomId, arrival, departure, 2, "Test Guest",
                "guest@example.com", "+41 79 000 00 00", BookingSource.RECEPTION)).getId();
    }

    /**
     * A stay that started in the past; booking cannot create it, so the dates are moved afterwards.
     */
    private long checkedInSince(long roomId, LocalDate arrival, LocalDate departure) {
        var id = book(roomId, today, today.plusDays(1));
        dsl.update(RESERVATION)
                .set(RESERVATION.ARRIVAL_DATE, arrival)
                .set(RESERVATION.DEPARTURE_DATE, departure)
                .set(RESERVATION.STATUS, ReservationStatus.CHECKED_IN.name())
                .where(RESERVATION.ID.eq(id)).execute();
        return id;
    }

    private String status(long id) {
        return dsl.fetchSingle(RESERVATION, RESERVATION.ID.eq(id)).getStatus();
    }
}
