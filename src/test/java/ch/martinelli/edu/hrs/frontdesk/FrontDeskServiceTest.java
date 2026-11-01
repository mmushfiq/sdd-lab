package ch.martinelli.edu.hrs.frontdesk;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService.RoomOverview;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService.RoomState;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService.DirtyRoom;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.AvailabilityService.AvailableRoom;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FrontDeskServiceTest {

    @Autowired
    DSLContext ctx;

    @Autowired
    HousekeepingService housekeepingService;

    @Autowired
    AvailabilityService availabilityService;

    private FrontDeskService on(LocalDate today) {
        return new FrontDeskService(ctx, Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
    }

    // Scenarios "Free room", "Arrival today", "Occupied room" and "Dirty room"
    @Test
    void overview_shows_state_guest_departure_and_cleaning_status() {
        insert(ctx, "ANNA0001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");
        insert(ctx, "BEN00001", "105", MARCH_8, MARCH_13, "Ben Keller", "CHECKED_IN");
        insert(ctx, "LATER001", "101", MARCH_11, MARCH_13, "Later Guest", "BOOKED");
        setDirty("106");

        var overview = on(MARCH_10).roomOverview();

        assertThat(overview).extracting(RoomOverview::roomNumber).startsWith("101", "102", "103", "104", "105")
                .hasSize(10);
        assertThat(room(overview, "101")).extracting(RoomOverview::state, RoomOverview::guestName,
                RoomOverview::cleaningStatus).containsExactly(RoomState.FREE, null, "CLEAN");
        assertThat(room(overview, "104")).extracting(RoomOverview::state, RoomOverview::guestName,
                RoomOverview::departureDate, RoomOverview::roomTypeName)
                .containsExactly(RoomState.ARRIVAL_TODAY, "Anna Meier", MARCH_12, "Double");
        assertThat(room(overview, "105")).extracting(RoomOverview::state, RoomOverview::guestName,
                RoomOverview::departureDate).containsExactly(RoomState.OCCUPIED, "Ben Keller", MARCH_13);
        assertThat(room(overview, "106").cleaningStatus()).isEqualTo("DIRTY");
    }

    // Scenario "Successful check-in"
    @Test
    void check_in_sets_checked_in_and_room_occupied() {
        ReservationRecord r = insert(ctx, "ANNA0001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");
        FrontDeskService service = on(MARCH_10);

        service.checkIn(r.getId());

        assertThat(reload(r).getStatus()).isEqualTo("CHECKED_IN");
        assertThat(room(service.roomOverview(), "104").state()).isEqualTo(RoomState.OCCUPIED);
    }

    // Scenario "Room is not clean"
    @Test
    void check_in_refused_for_dirty_room() {
        ReservationRecord r = insert(ctx, "ANNA0001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");
        setDirty("104");

        assertThatThrownBy(() -> on(MARCH_10).checkIn(r.getId())).hasMessage("The room is not clean yet");
        assertThat(reload(r).getStatus()).isEqualTo("BOOKED");
    }

    // Scenario "Room is still occupied"
    @Test
    void check_in_refused_while_previous_guest_checked_in() {
        insert(ctx, "BEN00001", "104", MARCH_8, MARCH_10, "Ben Keller", "CHECKED_IN");
        ReservationRecord next = insert(ctx, "ANNA0001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");

        assertThatThrownBy(() -> on(MARCH_10).checkIn(next.getId())).hasMessage("The room is still occupied");
        assertThat(reload(next).getStatus()).isEqualTo("BOOKED");
    }

    // Scenario "Not the arrival date": refused in the service too
    @Test
    void check_in_refused_on_other_day_or_when_not_booked() {
        ReservationRecord tomorrow = insert(ctx, "ANNA0001", "104", MARCH_11, MARCH_12, "Anna Meier", "BOOKED");
        ReservationRecord cancelled = insert(ctx, "CANCEL01", "105", MARCH_10, MARCH_12, "Ben", "CANCELLED");

        assertThatThrownBy(() -> on(MARCH_10).checkIn(tomorrow.getId()))
                .hasMessage(FrontDeskService.NOT_ARRIVAL_DATE);
        assertThatThrownBy(() -> on(MARCH_10).checkIn(cancelled.getId())).hasMessage(FrontDeskService.NOT_BOOKED);
    }

    // Scenario "Successful check-out"
    @Test
    void check_out_makes_room_dirty_and_listed_for_cleaning() {
        ReservationRecord r = insert(ctx, "BEN00001", "105", MARCH_10, MARCH_12, "Ben Keller", "CHECKED_IN");
        FrontDeskService service = on(MARCH_12);

        service.checkOut(r.getId());

        assertThat(reload(r).getStatus()).isEqualTo("CHECKED_OUT");
        assertThat(reload(r).getDepartureDate()).isEqualTo(MARCH_12);
        assertThat(housekeepingService.findDirtyRooms()).extracting(DirtyRoom::roomNumber).containsExactly("105");
        assertThat(room(service.roomOverview(), "105")).extracting(RoomOverview::state,
                RoomOverview::cleaningStatus).containsExactly(RoomState.FREE, "DIRTY");
    }

    // Scenario "Early check-out"
    @Test
    void early_check_out_shortens_stay_and_frees_room() {
        ReservationRecord r = insert(ctx, "BEN00001", "105", MARCH_10, MARCH_14, "Ben Keller", "CHECKED_IN");

        on(MARCH_12).checkOut(r.getId());

        assertThat(reload(r).getDepartureDate()).isEqualTo(MARCH_12);
        assertThat(availabilityService.findAvailableRooms(MARCH_12, MARCH_14, 2, null))
                .extracting(AvailableRoom::roomNumber).contains("105");
    }

    // Scenario "Check-out on the arrival day"
    @Test
    void check_out_on_arrival_day_keeps_one_night() {
        ReservationRecord r = insert(ctx, "BEN00001", "105", MARCH_10, MARCH_14, "Ben Keller", "CHECKED_IN");

        on(MARCH_10).checkOut(r.getId());

        assertThat(reload(r).getDepartureDate()).isEqualTo(MARCH_11);
    }

    // Scenario "Only Checked in reservations can be checked out": refused in the service too
    @Test
    void check_out_refused_when_not_checked_in() {
        ReservationRecord r = insert(ctx, "ANNA0001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");

        assertThatThrownBy(() -> on(MARCH_10).checkOut(r.getId())).hasMessage(FrontDeskService.NOT_CHECKED_IN);
        assertThat(reload(r).getStatus()).isEqualTo("BOOKED");
    }

    private static RoomOverview room(java.util.List<RoomOverview> overview, String number) {
        return overview.stream().filter(o -> o.roomNumber().equals(number)).findFirst().orElseThrow();
    }

    private void setDirty(String number) {
        ctx.update(ROOM).set(ROOM.CLEANING_STATUS, HousekeepingService.DIRTY)
                .where(ROOM.ROOM_NUMBER.eq(number)).execute();
    }

    private ReservationRecord reload(ReservationRecord r) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(r.getId()));
    }
}
