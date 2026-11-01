package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.reservation.AvailabilityService.AvailableRoom;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AvailabilityServiceTest {

    @Autowired
    AvailabilityService service;

    @Autowired
    DSLContext ctx;

    // Scenario "Free room is listed"
    @Test
    void free_room_is_listed() {
        assertThat(roomNumbers(MARCH_10, MARCH_12, 2)).contains("104");
    }

    // Scenario "Room with an overlapping reservation is not listed"
    @Test
    void room_with_overlapping_reservation_is_not_listed() {
        insert(ctx, "TEST0001", "104", MARCH_11, MARCH_13, "Anna Meier", ReservationService.BOOKED);

        assertThat(roomNumbers(MARCH_10, MARCH_12, 2)).doesNotContain("104").contains("105");
    }

    // Scenario "Departure day is free for the next guest"
    @Test
    void departure_day_is_free_for_next_guest() {
        insert(ctx, "TEST0001", "104", MARCH_8, MARCH_10, "Anna Meier", ReservationService.BOOKED);

        assertThat(roomNumbers(MARCH_10, MARCH_12, 2)).contains("104");
    }

    // Scenario "Cancelled reservation does not block the room"
    @Test
    void cancelled_reservation_does_not_block() {
        insert(ctx, "TEST0001", "104", MARCH_10, MARCH_12, "Anna Meier", ReservationService.CANCELLED);

        assertThat(roomNumbers(MARCH_10, MARCH_12, 2)).contains("104");
    }

    // Scenario "Room too small is not listed"
    @Test
    void room_too_small_is_not_listed() {
        List<AvailableRoom> rooms = service.findAvailableRooms(MARCH_10, MARCH_12, 3, null);

        assertThat(rooms).extracting(AvailableRoom::roomNumber).containsExactly("109", "110");
        assertThat(rooms).allSatisfy(r -> assertThat(r.capacity()).isGreaterThanOrEqualTo(3));
    }

    // Scenario "No room available"
    @Test
    void no_room_available_returns_empty_list() {
        insert(ctx, "TEST0001", "109", MARCH_10, MARCH_12, "Anna Meier", ReservationService.BOOKED);
        insert(ctx, "TEST0002", "110", MARCH_10, MARCH_12, "Ben Meier", ReservationService.BOOKED);

        assertThat(service.findAvailableRooms(MARCH_10, MARCH_12, 3, null)).isEmpty();
        assertThat(AvailabilityService.NO_ROOMS).isEqualTo("No rooms available for these dates");
    }

    // Requirement "Search result details", scenario "Total price shown"
    @Test
    void shows_details_and_total_price() {
        AvailableRoom room104 = service.findAvailableRooms(MARCH_10, MARCH_12, 2, null).stream()
                .filter(r -> r.roomNumber().equals("104")).findFirst().orElseThrow();

        assertThat(room104.roomTypeName()).isEqualTo("Double");
        assertThat(room104.capacity()).isEqualTo(2);
        assertThat(room104.nightlyRate()).isEqualByComparingTo("140.00");
        assertThat(room104.totalPrice()).isEqualByComparingTo("280.00");
    }

    // Scenario "Arrival date in the past"
    @Test
    void arrival_in_the_past_is_rejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThatThrownBy(() -> service.findAvailableRooms(yesterday, yesterday.plusDays(2), 2, null))
                .isInstanceOf(ReservationException.class).hasMessage("Arrival date cannot be in the past");
    }

    // Scenario "Departure not after arrival"
    @Test
    void departure_not_after_arrival_is_rejected() {
        assertThatThrownBy(() -> service.findAvailableRooms(MARCH_10, MARCH_10, 2, null))
                .hasMessage("Departure date must be after arrival date");
        assertThatThrownBy(() -> service.findAvailableRooms(MARCH_10, MARCH_8, 2, null))
                .hasMessage("Departure date must be after arrival date");
    }

    // Scenario "No guests"
    @Test
    void no_guests_is_rejected() {
        assertThatThrownBy(() -> service.findAvailableRooms(MARCH_10, MARCH_12, 0, null))
                .hasMessage("At least 1 guest is required");
        assertThatThrownBy(() -> service.findAvailableRooms(MARCH_10, MARCH_12, null, null))
                .hasMessage("At least 1 guest is required");
    }

    // The reservation being changed does not block its own room
    @Test
    void excluded_reservation_does_not_block() {
        long id = insert(ctx, "TEST0001", "104", MARCH_10, MARCH_12, "Anna Meier", ReservationService.BOOKED).getId();

        assertThat(service.findAvailableRooms(MARCH_10, MARCH_13, 2, id))
                .extracting(AvailableRoom::roomNumber).contains("104");
    }

    private List<String> roomNumbers(LocalDate arrival, LocalDate departure, int guests) {
        return service.findAvailableRooms(arrival, departure, guests, null).stream()
                .map(AvailableRoom::roomNumber).toList();
    }
}
