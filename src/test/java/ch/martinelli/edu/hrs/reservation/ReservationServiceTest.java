package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ReservationServiceTest {

    static final Guest ANNA = new Guest("Anna Meier", "anna@example.com", null);

    @Autowired
    ReservationService service;

    @Autowired
    DSLContext ctx;

    // Scenario "Reservation created" and requirement "Reservation content"
    @Test
    void creates_booked_reservation() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2),
                new Guest(" Anna Meier ", "anna@example.com", "+41 79 000 00 00"));

        ReservationRecord saved = ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(created.getId()));
        assertThat(saved.getStatus()).isEqualTo("BOOKED");
        assertThat(saved.getRoomId()).isEqualTo(roomId(ctx, "104"));
        assertThat(saved.getArrivalDate()).isEqualTo(MARCH_10);
        assertThat(saved.getDepartureDate()).isEqualTo(MARCH_12);
        assertThat(saved.getNumberOfGuests()).isEqualTo(2);
        assertThat(saved.getGuestName()).isEqualTo("Anna Meier");
        assertThat(saved.getGuestEmail()).isEqualTo("anna@example.com");
        assertThat(saved.getGuestPhone()).isEqualTo("+41 79 000 00 00");
        assertThat(saved.getNightlyRate()).isEqualByComparingTo("140.00");
        assertThat(saved.getReservationNumber()).isEqualTo(created.getReservationNumber());
    }

    // Scenario "Reservation number format"
    @Test
    void reservation_numbers_have_the_format_and_are_unique() {
        ReservationRecord first = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        ReservationRecord second = service.create(stay("105", MARCH_10, MARCH_12, 2), ANNA);

        assertThat(first.getReservationNumber()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        assertThat(second.getReservationNumber()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}")
                .isNotEqualTo(first.getReservationNumber());
    }

    // Scenario "Overlapping reservation rejected"
    @Test
    void overlapping_reservation_is_rejected() {
        service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        assertThatThrownBy(() -> service.create(stay("104", MARCH_11, MARCH_14, 2), ANNA))
                .isInstanceOf(ReservationException.class)
                .hasMessage("The room is no longer available for these dates");
        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(1);
    }

    // Scenario "Back-to-back reservations allowed"
    @Test
    void back_to_back_reservations_are_allowed() {
        service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        service.create(stay("104", MARCH_12, MARCH_14, 2), ANNA);

        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(2);
    }

    // Scenario "Missing guest data" at service level (the service is reused by online booking)
    @Test
    void missing_guest_data_is_rejected() {
        assertThatThrownBy(() -> service.create(stay("104", MARCH_10, MARCH_12, 2),
                new Guest(" ", "anna@example.com", null))).hasMessage("Guest name is required");
        assertThatThrownBy(() -> service.create(stay("104", MARCH_10, MARCH_12, 2),
                new Guest("Anna Meier", null, null))).hasMessage("Email is required");
        assertThat(ctx.fetchCount(RESERVATION)).isZero();
    }

    // Scenario "Too many guests for the room"
    @Test
    void too_many_guests_is_rejected() {
        assertThatThrownBy(() -> service.create(stay("104", MARCH_10, MARCH_12, 3), ANNA))
                .hasMessage("Too many guests for this room");
    }

    // Requirement "Valid search input" also applies when saving
    @Test
    void invalid_dates_are_rejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> service.create(stay("104", yesterday, yesterday.plusDays(2), 2), ANNA))
                .hasMessage("Arrival date cannot be in the past");
        assertThatThrownBy(() -> service.create(stay("104", MARCH_12, MARCH_10, 2), ANNA))
                .hasMessage("Departure date must be after arrival date");
    }

    // Scenario "Rate change after booking"
    @Test
    void rate_is_kept_after_rate_change() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("150.00"))
                .where(ROOM_TYPE.NAME.eq("Double")).execute();

        assertThat(reload(created).getNightlyRate()).isEqualByComparingTo("140.00");
    }

    // Scenario "Moved to another room type"
    @Test
    void move_to_another_room_type_takes_its_current_rate() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        service.change(created.getId(), stay("109", MARCH_10, MARCH_12, 2));

        ReservationRecord changed = reload(created);
        assertThat(changed.getRoomId()).isEqualTo(roomId(ctx, "109"));
        assertThat(changed.getNightlyRate()).isEqualByComparingTo("220.00");
    }

    // A move within the same room type keeps the saved rate
    @Test
    void move_within_room_type_keeps_saved_rate() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal("150.00"))
                .where(ROOM_TYPE.NAME.eq("Double")).execute();

        service.change(created.getId(), stay("105", MARCH_10, MARCH_12, 2));

        assertThat(reload(created).getNightlyRate()).isEqualByComparingTo("140.00");
    }

    // Scenario "Dates extended": the reservation itself does not count as an overlap
    @Test
    void dates_extended() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        service.change(created.getId(), stay("104", MARCH_10, MARCH_13, 2));

        assertThat(reload(created).getDepartureDate()).isEqualTo(MARCH_13);
    }

    // Scenario "Change creates an overlap"
    @Test
    void change_creating_overlap_is_rejected() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        service.create(stay("104", MARCH_12, MARCH_14, 2), ANNA);

        assertThatThrownBy(() -> service.change(created.getId(), stay("104", MARCH_10, MARCH_13, 2)))
                .hasMessage("The room is no longer available for these dates");
        assertThat(reload(created).getDepartureDate()).isEqualTo(MARCH_12);
    }

    // Scenario "Reservation cancelled": the room is available again
    @Test
    void cancel_frees_the_room() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        service.cancel(created.getId());

        assertThat(reload(created).getStatus()).isEqualTo("CANCELLED");
        service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
    }

    // Scenarios "Reservation is not Booked" and "Only Booked reservations can be cancelled"
    @Test
    void change_and_cancel_of_non_booked_reservation_are_rejected() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        service.cancel(created.getId());

        assertThatThrownBy(() -> service.change(created.getId(), stay("104", MARCH_10, MARCH_13, 2)))
                .hasMessage(ReservationService.NOT_BOOKED);
        assertThatThrownBy(() -> service.cancel(created.getId()))
                .hasMessage(ReservationService.NOT_BOOKED);
    }

    // Requirement "List reservations": departure today or later, sorted by arrival, search by number or name
    @Test
    void finds_upcoming_reservations() {
        LocalDate today = LocalDate.now();
        insert(ctx, "PAST0001", "101", today.minusDays(3), today.minusDays(1), "Old Meier", "BOOKED");
        insert(ctx, "TODAY001", "102", today.minusDays(1), today, "Carl Keller", "BOOKED");
        insert(ctx, "LATER001", "103", MARCH_10, MARCH_12, "Anna Meier", "CANCELLED");

        assertThat(service.findUpcoming(null)).extracting(ReservationRecord::getReservationNumber)
                .containsExactly("TODAY001", "LATER001");
        assertThat(service.findUpcoming("meier")).extracting(ReservationRecord::getReservationNumber)
                .containsExactly("LATER001");
        assertThat(service.findUpcoming("today")).extracting(ReservationRecord::getReservationNumber)
                .containsExactly("TODAY001");
    }

    private Stay stay(String roomNumber, LocalDate arrival, LocalDate departure, int guests) {
        return new Stay(roomId(ctx, roomNumber), arrival, departure, guests);
    }

    private ReservationRecord reload(ReservationRecord r) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(r.getId()));
    }
}
