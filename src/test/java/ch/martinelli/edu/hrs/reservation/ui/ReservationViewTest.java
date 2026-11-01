package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.ui.AccessDeniedView;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.reservation.ReservationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

@Transactional
@WithUserDetails("reception")
class ReservationViewTest extends HrsBrowserlessTest {

    @Autowired
    ReservationService reservationService;

    @Autowired
    DSLContext ctx;

    @MockitoBean
    JavaMailSender mailSender;

    Grid<ReservationRecord> grid;

    @SuppressWarnings("unchecked")
    private void open() {
        navigate(ReservationView.class);
        grid = $(Grid.class).id("reservations");
    }

    // Scenario "Reservation created"
    @Test
    void creates_reservation_and_shows_its_number() {
        open();
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 2);
        chooseRoom("104");
        test(field("Guest name")).setValue("Anna Meier");
        test(field("Email")).setValue("anna@example.com");
        click("create-reservation");

        ReservationRecord saved = ctx.fetchSingle(RESERVATION);
        assertThat(saved.getStatus()).isEqualTo("BOOKED");
        assertThat(saved.getRoomId()).isEqualTo(roomId(ctx, "104"));
        assertThat(saved.getNumberOfGuests()).isEqualTo(2);
        assertThat(lastNotification()).isEqualTo("Reservation " + saved.getReservationNumber() + " created");
        assertThat($(Dialog.class).exists()).isFalse();
        assertThat(rows()).containsExactly(
                saved.getReservationNumber() + "|Anna Meier|104|2030-03-10|2030-03-12|Booked");
        // Scenario "Reservation made by reception": no confirmation email
        verifyNoInteractions(mailSender);
    }

    // Requirement "Search result details", scenario "Total price shown"
    @Test
    void search_result_shows_room_details_and_total_price() {
        open();
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 2);

        Grid<AvailableRoom> rooms = availableRooms();
        int row = rowOf(rooms, "104");
        assertThat(IntStream.range(0, 5).mapToObj(c -> test(rooms).getCellText(row, c)).toList())
                .containsExactly("104", "Double", "2", "CHF 140.00", "CHF 280.00");
    }

    // Scenario "Missing guest data"
    @Test
    void missing_guest_data_marks_the_fields() {
        open();
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 2);
        chooseRoom("104");
        click("create-reservation");

        assertThat(field("Guest name").isInvalid()).isTrue();
        assertThat(field("Guest name").getErrorMessage()).isEqualTo("Guest name is required");
        assertThat(field("Email").isInvalid()).isTrue();
        assertThat(field("Email").getErrorMessage()).isEqualTo("Email is required");
        assertThat(ctx.fetchCount(RESERVATION)).isZero();
    }

    // Scenario "Invalid email"
    @Test
    void invalid_email_is_rejected() {
        open();
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 2);
        chooseRoom("104");
        test(field("Guest name")).setValue("Anna Meier");
        test(field("Email")).setValue("anna-at-example.com");
        click("create-reservation");

        assertThat(field("Email").isInvalid()).isTrue();
        assertThat(field("Email").getErrorMessage()).isEqualTo("Enter a valid email address");
        assertThat(ctx.fetchCount(RESERVATION)).isZero();
    }

    // Scenario "No room available"
    @Test
    void no_room_available_shows_message() {
        open();
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 5);

        assertThat($(Span.class).id("availability-message").getText()).isEqualTo("No rooms available for these dates");
        assertThat($(Grid.class).withId("available-rooms").exists()).isFalse();
    }

    // Requirement "Valid search input"
    @Test
    void invalid_search_input_shows_message() {
        open();
        click("new-reservation");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        searchAvailability(yesterday, yesterday.plusDays(2), 2);
        assertThat(availabilityMessage()).isEqualTo("Arrival date cannot be in the past");

        searchAvailability(MARCH_12, MARCH_10, 2);
        assertThat(availabilityMessage()).isEqualTo("Departure date must be after arrival date");

        searchAvailability(MARCH_10, MARCH_12, 0);
        assertThat(availabilityMessage()).isEqualTo("At least 1 guest is required");
    }

    // Scenario "Upcoming reservations listed": departure today or later, sorted by arrival date
    @Test
    void lists_only_upcoming_reservations_sorted_by_arrival() {
        LocalDate today = LocalDate.now();
        insert(ctx, "LATER001", "105", MARCH_10, MARCH_12, "Ben Keller", "BOOKED");
        insert(ctx, "PAST0001", "101", today.minusDays(3), today.minusDays(1), "Old Guest", "BOOKED");
        insert(ctx, "TODAY001", "102", today.minusDays(1), today, "Carl Keller", "CANCELLED");
        open();

        assertThat(rows()).containsExactly(
                "TODAY001|Carl Keller|102|" + today.minusDays(1) + "|" + today + "|Cancelled",
                "LATER001|Ben Keller|105|2030-03-10|2030-03-12|Booked");
    }

    // Scenario "Checked in reservation shown in the reservation list"
    @Test
    void shows_checked_in_and_checked_out_statuses() {
        LocalDate today = LocalDate.now();
        insert(ctx, "CHECKIN1", "104", today, today.plusDays(2), "Anna Meier", "CHECKED_IN");
        insert(ctx, "CHECKOU1", "105", today.minusDays(1), today, "Ben Keller", "CHECKED_OUT");
        open();

        assertThat(rows()).extracting(r -> r.substring(r.lastIndexOf('|') + 1))
                .containsExactly("Checked out", "Checked in");
    }

    // Scenario "Search by guest name"
    @Test
    void search_by_guest_name() {
        insert(ctx, "MEIER001", "104", MARCH_10, MARCH_12, "Anna Meier", "BOOKED");
        insert(ctx, "KELLER01", "105", MARCH_10, MARCH_12, "Ben Keller", "BOOKED");
        open();

        test($(TextField.class).id("reservation-search")).setValue("Meier");

        assertThat(rows()).extracting(r -> r.split("\\|")[1]).containsExactly("Anna Meier");
    }

    // Scenario "Reservation details shown"
    @Test
    void shows_reservation_details() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);

        assertThat($(Paragraph.class).from($(Div.class).id("reservation-details")).all())
                .extracting(Paragraph::getText).containsExactly(
                        "Reservation number: " + r.getReservationNumber(), "Status: Booked", "Room: 104",
                        "Arrival: 2030-03-10", "Departure: 2030-03-12", "Guests: 2", "Guest name: Anna Meier",
                        "Email: anna@example.com", "Phone: +41 79 000 00 00", "Nightly rate: CHF 140.00",
                        "Total price: CHF 280.00");
    }

    // Scenario "Dates extended"
    @Test
    void changes_dates() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);
        assertThat(availableRooms().asSingleSelect().getValue().roomNumber()).isEqualTo("104");

        searchAvailability(MARCH_10, MARCH_13, 2);
        chooseRoom("104");
        click("save-reservation");

        assertThat(reload(r).getDepartureDate()).isEqualTo(MARCH_13);
        assertThat(lastNotification()).isEqualTo("Reservation " + r.getReservationNumber() + " changed");
    }

    // Scenario "Moved to another room type"
    @Test
    void move_to_family_room_takes_its_rate() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);
        chooseRoom("109");
        click("save-reservation");

        assertThat(reload(r).getNightlyRate()).isEqualByComparingTo("220.00");
        assertThat(rows().getFirst()).contains("|109|");
    }

    // Scenario "Change creates an overlap": room taken between search and save
    @Test
    void change_creating_overlap_shows_message() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);
        searchAvailability(MARCH_10, MARCH_13, 2);
        chooseRoom("104");
        book("104", MARCH_12, MARCH_14);
        click("save-reservation");

        assertThat(lastNotification()).isEqualTo("The room is no longer available for these dates");
        assertThat(reload(r).getDepartureDate()).isEqualTo(MARCH_12);
    }

    // Scenario "Reservation cancelled"
    @Test
    void cancels_after_confirmation() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);
        click("cancel-reservation");
        test($(ConfirmDialog.class).single()).confirm();

        assertThat(reload(r).getStatus()).isEqualTo("CANCELLED");
        assertThat(rows().getFirst()).endsWith("|Cancelled");
        click("new-reservation");
        searchAvailability(MARCH_10, MARCH_12, 2);
        assertThat(rowOf(availableRooms(), "104")).isNotNegative();
    }

    // Scenario "Cancellation not confirmed"
    @Test
    void cancellation_not_confirmed_keeps_reservation() {
        ReservationRecord r = book("104", MARCH_10, MARCH_12);
        open();
        test(grid).clickRow(0);
        click("cancel-reservation");
        test($(ConfirmDialog.class).single()).cancel();

        assertThat(reload(r).getStatus()).isEqualTo("BOOKED");
    }

    // Scenarios "Reservation is not Booked" and "Only Booked reservations can be cancelled"
    @Test
    void cancelled_reservation_offers_no_change_or_cancel() {
        insert(ctx, "CANCEL01", "104", MARCH_10, MARCH_12, "Anna Meier", "CANCELLED");
        open();
        test(grid).clickRow(0);

        assertThat($(Div.class).id("reservation-details").isVisible()).isTrue();
        assertThat($(Button.class).withId("save-reservation").exists()).isFalse();
        assertThat($(Button.class).withId("cancel-reservation").exists()).isFalse();
        assertThat($(Button.class).withId("search-availability").exists()).isFalse();
    }

    // Scenario "Housekeeping opens the reservations page"
    @Test
    @WithUserDetails("housekeeping")
    void housekeeping_gets_access_denied() {
        navigate(ReservationView.ROUTE, AccessDeniedView.class);

        assertThat($(H1.class).single().getText()).isEqualTo(AccessDeniedView.TEXT);
        assertThat($(Grid.class).exists()).isFalse();
    }

    private ReservationRecord book(String room, LocalDate arrival, LocalDate departure) {
        return reservationService.create(new Stay(roomId(ctx, room), arrival, departure, 2),
                new Guest("Anna Meier", "anna@example.com", "+41 79 000 00 00"));
    }

    private ReservationRecord reload(ReservationRecord r) {
        return ctx.fetchSingle(RESERVATION, RESERVATION.ID.eq(r.getId()));
    }

    private void searchAvailability(LocalDate arrival, LocalDate departure, int guests) {
        test($(DatePicker.class).withLabel("Arrival").single()).setValue(arrival);
        test($(DatePicker.class).withLabel("Departure").single()).setValue(departure);
        test($(IntegerField.class).withLabel("Guests").single()).setValue(guests);
        click("search-availability");
    }

    private String availabilityMessage() {
        return $(Span.class).id("availability-message").getText();
    }

    @SuppressWarnings("unchecked")
    private Grid<AvailableRoom> availableRooms() {
        return $(Grid.class).id("available-rooms");
    }

    private void chooseRoom(String roomNumber) {
        Grid<AvailableRoom> rooms = availableRooms();
        test(rooms).select(rowOf(rooms, roomNumber));
    }

    private int rowOf(Grid<AvailableRoom> rooms, String roomNumber) {
        return IntStream.range(0, test(rooms).size())
                .filter(i -> test(rooms).getRow(i).roomNumber().equals(roomNumber))
                .findFirst().orElse(-1);
    }

    private TextField field(String label) {
        return $(TextField.class).withLabel(label).single();
    }

    private void click(String id) {
        test($(Button.class).id(id)).click();
    }

    private String lastNotification() {
        return test($(Notification.class).last()).getText();
    }

    private List<String> rows() {
        return IntStream.range(0, test(grid).size())
                .mapToObj(i -> IntStream.range(0, 6).mapToObj(c -> test(grid).getCellText(i, c))
                        .reduce((a, b) -> a + "|" + b).orElseThrow())
                .toList();
    }
}
