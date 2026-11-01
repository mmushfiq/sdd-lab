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
import java.time.LocalDateTime;
import java.util.List;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_TYPE;
import static ch.martinelli.edu.hrs.db.Tables.SEASONAL_RATE;
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
        assertThat(rates(created)).containsExactly("140.00", "140.00");
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

    // Scenario "Stay across the start of a season"
    @Test
    void stay_across_the_start_of_a_season_saves_the_rate_of_each_night() {
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");

        ReservationRecord created = service.create(stay("104", DECEMBER_18, DECEMBER_21, 2), ANNA);

        assertThat(rates(created)).containsExactly("140.00", "140.00", "180.00");
        assertThat(AvailabilityService.totalPrice(service.nights(created.getId()))).isEqualByComparingTo("460.00");
    }

    // Scenario "Rate change after booking"
    @Test
    void rate_is_kept_after_rate_change() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        setBaseRate("Double", "150.00");

        assertThat(rates(created)).containsExactly("140.00", "140.00");
    }

    // Requirement "Change a seasonal rate", scenario "Existing reservation keeps its rates"
    @Test
    void rates_are_kept_after_seasonal_rate_change() {
        long christmas = insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        ReservationRecord created = service.create(stay("104", DECEMBER_20, DECEMBER_21, 2), ANNA);

        ctx.update(SEASONAL_RATE).set(SEASONAL_RATE.NIGHTLY_RATE, new BigDecimal("190.00"))
                .where(SEASONAL_RATE.ID.eq(christmas)).execute();

        assertThat(rates(created)).containsExactly("180.00");
    }

    // Requirement "Delete a seasonal rate", scenario "Reservation after deletion unchanged"
    @Test
    void rates_are_kept_after_seasonal_rate_deletion() {
        long christmas = insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");
        ReservationRecord created = service.create(stay("104", DECEMBER_20, DECEMBER_21, 2), ANNA);

        ctx.deleteFrom(SEASONAL_RATE).where(SEASONAL_RATE.ID.eq(christmas)).execute();

        assertThat(rates(created)).containsExactly("180.00");
    }

    // Scenario "Moved to another room type"
    @Test
    void move_to_another_room_type_takes_its_current_rates() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        service.change(created.getId(), stay("109", MARCH_10, MARCH_12, 2));

        assertThat(reload(created).getRoomId()).isEqualTo(roomId(ctx, "109"));
        assertThat(rates(created)).containsExactly("220.00", "220.00");
    }

    // A move within the same room type keeps the saved rates
    @Test
    void move_within_room_type_keeps_saved_rates() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        setBaseRate("Double", "150.00");

        service.change(created.getId(), stay("105", MARCH_10, MARCH_12, 2));

        assertThat(rates(created)).containsExactly("140.00", "140.00");
    }

    // Scenario "Extended into a season"
    @Test
    void extended_into_a_season_keeps_old_nights_and_prices_new_ones() {
        setBaseRate("Double", "130.00");
        ReservationRecord created = service.create(stay("104", DECEMBER_18, DECEMBER_20, 2), ANNA);
        setBaseRate("Double", "140.00");
        insertSeasonalRate(ctx, "Double", "Christmas", DECEMBER_20, JANUARY_6, "180.00");

        service.change(created.getId(), stay("104", DECEMBER_18, LocalDate.of(2030, 12, 22), 2));

        assertThat(rates(created)).containsExactly("130.00", "130.00", "180.00", "180.00");
    }

    // A shortened stay no longer has the removed nights
    @Test
    void shortened_stay_drops_the_removed_nights() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_13, 2), ANNA);

        service.change(created.getId(), stay("104", MARCH_11, MARCH_13, 2));

        assertThat(service.nights(created.getId())).extracting(AvailabilityService.NightRate::night)
                .containsExactly(MARCH_11, MARCH_12);
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

    // Requirement "Guest finds their reservation": number and email ignore case and surrounding spaces
    @Test
    void finds_reservation_for_guest() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        String number = created.getReservationNumber();

        assertThat(service.findForGuest(number, "anna@example.com")).hasValueSatisfying(g -> {
            assertThat(g.reservation().getId()).isEqualTo(created.getId());
            assertThat(g.roomTypeName()).isEqualTo("Double");
        });
        assertThat(service.findForGuest(" " + number.toLowerCase() + " ", " ANNA@Example.com ")).isPresent();
    }

    // Scenario "Wrong reservation number or email": both give the same empty result
    @Test
    void unknown_number_or_wrong_email_finds_nothing() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        assertThat(service.findForGuest("ZZZZZZZZ", "anna@example.com")).isEmpty();
        assertThat(service.findForGuest(created.getReservationNumber(), "other@example.com")).isEmpty();
        assertThat(service.findForGuest(null, null)).isEmpty();
    }

    // Scenario "Guest cancels before the arrival date"
    @Test
    void guest_cancels_before_arrival() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        service.cancelByGuest(created.getReservationNumber(), "Anna@example.com");

        assertThat(reload(created).getStatus()).isEqualTo("CANCELLED");
        service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
    }

    // Scenario "Cancel on or after the arrival date": checked in the service, not only in the view
    @Test
    void guest_cannot_cancel_on_or_after_arrival_day() {
        LocalDate today = LocalDate.now();
        ReservationRecord arrivesToday = insert(ctx, "TODAY001", "104", today, today.plusDays(2), "Anna", "BOOKED");
        ReservationRecord arrivedYesterday = insert(ctx, "YESTER01", "105", today.minusDays(1), today.plusDays(1),
                "Anna", "BOOKED");

        assertThatThrownBy(() -> service.cancelByGuest("TODAY001", "guest@example.com"))
                .hasMessage("The online cancellation deadline has passed. Please contact the hotel to cancel.");
        assertThatThrownBy(() -> service.cancelByGuest("YESTER01", "guest@example.com"))
                .hasMessage("The online cancellation deadline has passed. Please contact the hotel to cancel.");
        assertThat(reload(arrivesToday).getStatus()).isEqualTo("BOOKED");
        assertThat(reload(arrivedYesterday).getStatus()).isEqualTo("BOOKED");
    }

    // Requirement "Online cancellation deadline", scenario "New reservation"
    @Test
    void new_reservation_has_48_hour_deadline() {
        ReservationRecord created = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);

        assertThat(reload(created).getCancellationHoursBefore()).isEqualTo(48);
        assertThat(ReservationService.onlineCancellationDeadline(created))
                .isEqualTo(LocalDateTime.of(2030, 3, 8, 14, 0));
    }

    // Scenario "Reservation from before the rule": migration V6 gave existing rows 14 hours
    @Test
    void reservation_from_before_the_rule_has_midnight_deadline() {
        ReservationRecord old = oldRule(insert(ctx, "OLDRULE1", "104", MARCH_10, MARCH_12, "Anna", "BOOKED"));

        assertThat(ReservationService.onlineCancellationDeadline(old)).isEqualTo(LocalDateTime.of(2030, 3, 10, 0, 0));
    }

    // Requirement "Guest cancels their reservation": cancel allowed until one minute before the deadline, not at it
    @Test
    void guest_may_cancel_only_before_the_deadline() {
        ReservationRecord newRule = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        ReservationRecord old = oldRule(insert(ctx, "OLDRULE1", "105", MARCH_10, MARCH_12, "Anna", "BOOKED"));

        assertThat(ReservationService.guestMayCancel(newRule, LocalDateTime.of(2030, 3, 8, 13, 59))).isTrue();
        assertThat(ReservationService.guestMayCancel(newRule, LocalDateTime.of(2030, 3, 8, 14, 0))).isFalse();
        // Scenario "Reservation from before the rule cancelled the day before arrival"
        assertThat(ReservationService.guestMayCancel(old, LocalDateTime.of(2030, 3, 9, 20, 0))).isTrue();
        assertThat(ReservationService.guestMayCancel(old, LocalDateTime.of(2030, 3, 9, 23, 59))).isTrue();
        assertThat(ReservationService.guestMayCancel(old, LocalDateTime.of(2030, 3, 10, 0, 0))).isFalse();
    }

    // Scenario "Cancel after the deadline but before the arrival date": checked in the service
    @Test
    void guest_cannot_cancel_after_deadline_before_arrival() {
        LocalDate tomorrow = LocalDate.now(ReservationService.HOTEL_ZONE).plusDays(1);
        ReservationRecord r = insert(ctx, "TOMORR01", "104", tomorrow, tomorrow.plusDays(2), "Anna", "BOOKED");

        assertThatThrownBy(() -> service.cancelByGuest("TOMORR01", "guest@example.com"))
                .hasMessage("The online cancellation deadline has passed. Please contact the hotel to cancel.");
        assertThat(reload(r).getStatus()).isEqualTo("BOOKED");
    }

    // Requirement "Changed reservation keeps its deadline rule"
    @Test
    void changed_reservation_keeps_its_deadline_rule() {
        LocalDate march20 = LocalDate.of(2030, 3, 20);
        ReservationRecord newRule = service.create(stay("104", MARCH_10, MARCH_12, 2), ANNA);
        ReservationRecord old = oldRule(insert(ctx, "OLDRULE1", "105", MARCH_10, MARCH_12, "Anna", "BOOKED"));

        service.change(newRule.getId(), stay("104", march20, march20.plusDays(2), 2));
        service.change(old.getId(), stay("105", march20, march20.plusDays(2), 1));

        assertThat(ReservationService.onlineCancellationDeadline(reload(newRule)))
                .isEqualTo(LocalDateTime.of(2030, 3, 18, 14, 0));
        assertThat(ReservationService.onlineCancellationDeadline(reload(old)))
                .isEqualTo(LocalDateTime.of(2030, 3, 20, 0, 0));
    }

    @Test
    void cancellation_text_shows_the_exact_deadline() {
        assertThat(ReservationService.cancellationText(LocalDateTime.of(2030, 3, 8, 14, 0)))
                .isEqualTo("Free online cancellation until 8 March 2030, 14:00 (Zurich time)");
    }

    // Scenario "Cancelled reservation found": no second cancel; unknown reservation rejected
    @Test
    void guest_cannot_cancel_cancelled_or_unknown_reservation() {
        insert(ctx, "CANCEL01", "104", MARCH_10, MARCH_12, "Anna", "CANCELLED");

        assertThatThrownBy(() -> service.cancelByGuest("CANCEL01", "guest@example.com"))
                .hasMessage(ReservationService.NOT_BOOKED);
        assertThatThrownBy(() -> service.cancelByGuest("CANCEL01", "other@example.com"))
                .hasMessage("No reservation found for this reservation number and email");
    }

    private Stay stay(String roomNumber, LocalDate arrival, LocalDate departure, int guests) {
        return new Stay(roomId(ctx, roomNumber), arrival, departure, guests);
    }

    private ReservationRecord oldRule(ReservationRecord r) {
        ctx.update(RESERVATION).set(RESERVATION.CANCELLATION_HOURS_BEFORE, 14).where(RESERVATION.ID.eq(r.getId()))
                .execute();
        return reload(r);
    }

    private List<String> rates(ReservationRecord r) {
        return service.nights(r.getId()).stream().map(n -> n.rate().setScale(2).toPlainString()).toList();
    }

    private void setBaseRate(String roomTypeName, String rate) {
        ctx.update(ROOM_TYPE).set(ROOM_TYPE.NIGHTLY_RATE, new BigDecimal(rate))
                .where(ROOM_TYPE.NAME.eq(roomTypeName)).execute();
    }

    private ReservationRecord reload(ReservationRecord r) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(r.getId()));
    }
}
