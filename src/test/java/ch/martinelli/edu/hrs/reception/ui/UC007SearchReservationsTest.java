package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reservation.ReservationSearchResult;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.GUEST;
import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-007 "Search Reservations". Test data: V105__uc007_test_data.sql, room types from
 * V100__uc001_test_data.sql and staff members from V103__uc022_test_data.sql (password Secret-123).
 */
@WithAnonymousUser
class UC007SearchReservationsTest extends HrsBrowserlessTest {

    @Autowired
    DSLContext ctx;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-001", "BR-002"})
    void receptionist_finds_reservations_by_guest_name_and_opens_one() {
        Result<Record> dataBefore = allReservationAndGuestData();
        logIn("rita");

        // Step 1
        test($(RouterLink.class).withText("Reservations").single()).click();
        assertThat(getCurrentView()).isInstanceOf(SearchReservationsView.class);

        // Step 2: part of the last name, in another case
        search("such", null, null);

        // Step 3: ordered by arrival date, every status and channel
        Grid<ReservationSearchResult> grid = grid();
        assertThat(codes(grid)).containsExactly("SEARCH0003", "SEARCH0002", "SEARCH0001");
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText).containsExactly(
                "Reservation code", "Guest", "Arrival", "Departure", "Room type", "Guests", "Status");
        assertThat(IntStream.range(0, 7).mapToObj(column -> test(grid).getCellText(2, column)).toList())
                .containsExactly("SEARCH0001", "Sophie Suchmann", "Apr 10, 2031", "Apr 12, 2031", "Double Room", "2",
                        "Confirmed");
        assertThat(test(grid).getCellText(0, 6)).isEqualTo("Checked In");
        assertThat(test(grid).getCellText(1, 6)).isEqualTo("Cancelled");

        // Step 4
        test(grid).select(2);

        // Step 5: all data, the guest's email address and phone number, and the actions the status allows
        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat($(H1.class).single().getText()).isEqualTo("Reservation SEARCH0001");
        assertThat(field("status")).isEqualTo("Status: Confirmed");
        assertThat(field("arrival-date")).isEqualTo("Arrival date: Apr 10, 2031");
        assertThat(field("departure-date")).isEqualTo("Departure date: Apr 12, 2031");
        assertThat(field("room-type")).isEqualTo("Room type: Double Room");
        assertThat(field("number-of-guests")).isEqualTo("Number of guests: 2");
        assertThat(field("total-price")).isEqualTo("Total price: CHF 300.00");
        assertThat(field("channel")).isEqualTo("Channel: Reception");
        assertThat(field("guest-name")).isEqualTo("Name: Sophie Suchmann");
        assertThat(field("guest-email")).isEqualTo("Email address: sophie.suchmann@example.com");
        assertThat(field("guest-phone")).isEqualTo("Phone number: +41 79 150 15 01");
        assertThat($(Paragraph.class).withId("room").exists()).isFalse();
        assertThat(actions()).containsExactly("Change", "Cancel reservation", "Check in", "Record no-show");

        // Success postcondition: no reservation or guest data is changed
        assertThat(allReservationAndGuestData()).isEqualTo(dataBefore);
    }

    @Test
    @UseCase(id = "UC-007")
    void checked_in_reservation_shows_the_assigned_room_and_its_actions() {
        logIn("rita");
        navigate(SearchReservationsView.class);
        search(null, "SEARCH0003", null);
        test(grid()).select(0);

        assertThat(field("status")).isEqualTo("Status: Checked In");
        assertThat(field("room")).isEqualTo("Room: 401");
        assertThat(field("checked-in-at")).startsWith("Checked in: May 31, 2030");
        assertThat(actions()).containsExactly("Change", "Check out");
    }

    @Test
    @UseCase(id = "UC-007")
    void cancelled_reservation_allows_no_action() {
        logIn("rita");
        navigate(SearchReservationsView.class);
        search(null, "SEARCH0002", null);
        test(grid()).select(0);

        assertThat(field("status")).isEqualTo("Status: Cancelled");
        assertThat(field("channel")).isEqualTo("Channel: Online");
        assertThat(field("cancelled-at")).startsWith("Cancelled: May 2, 2030");
        assertThat($(HorizontalLayout.class).withId("actions").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // BR-001: Search criteria
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-001"})
    void guest_name_matches_part_of_the_first_name_case_insensitively() {
        openSearch();

        search("OPHI", null, null);

        assertThat(codes(grid())).containsExactly("SEARCH0003", "SEARCH0002", "SEARCH0001");
    }

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-001"})
    void reservation_code_matches_only_exactly() {
        openSearch();

        search(null, "SEARCH0001", null);
        assertThat(codes(grid())).containsExactly("SEARCH0001");

        search(null, "SEARCH000", null);
        assertThat($(Grid.class).exists()).isFalse();
        assertThat(message()).isEqualTo(SearchReservationsView.NO_MATCH);

        search(null, "search0001", null);
        assertThat(message()).isEqualTo(SearchReservationsView.NO_MATCH);
    }

    @Test
    @UseCase(id = "UC-007", businessRules = {"BR-001"})
    void date_matches_reservations_from_arrival_date_to_departure_date() {
        openSearch();

        // Arrival date, a day of the stay and the departure date match
        for (LocalDate date : List.of(LocalDate.of(2031, 4, 10), LocalDate.of(2031, 4, 11),
                LocalDate.of(2031, 4, 12))) {
            search(null, null, date);
            assertThat(codes(grid())).as("date %s", date).contains("SEARCH0001");
        }

        // The days before arrival and after departure do not
        search("Suchmann", null, LocalDate.of(2031, 4, 9));
        assertThat($(Grid.class).exists()).isFalse();
        search("Suchmann", null, LocalDate.of(2031, 4, 13));
        assertThat($(Grid.class).exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-007", scenario = "A4: Search Refined", businessRules = {"BR-001"})
    void combined_criteria_must_all_match() {
        openSearch();
        search("Suchmann", null, null);
        assertThat(codes(grid())).hasSize(3);

        // A4: the receptionist combines the criteria
        search("Suchmann", null, LocalDate.of(2031, 4, 5));

        assertThat(codes(grid())).containsExactly("SEARCH0002");

        search("Suchmann", "SEARCH0001", LocalDate.of(2031, 4, 5));
        assertThat(message()).isEqualTo(SearchReservationsView.NO_MATCH);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: No search criterion
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007", scenario = "A1: No Search Criterion")
    void receptionist_is_asked_for_at_least_one_criterion() {
        openSearch();

        search(null, null, null);
        assertThat(message()).isEqualTo(SearchReservationsView.NO_CRITERION);
        assertThat($(Grid.class).exists()).isFalse();

        // Text of only spaces is no criterion
        search("   ", "  ", null);
        assertThat(message()).isEqualTo(SearchReservationsView.NO_CRITERION);

        // A1 step 2: continues at step 2
        search("Suchmann", null, null);
        assertThat(codes(grid())).hasSize(3);
        assertThat($(Paragraph.class).withId("search-message").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: No matching reservation
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007", scenario = "A2: No Matching Reservation")
    void receptionist_is_informed_when_no_reservation_matches() {
        openSearch();

        search("Niemand", null, null);

        assertThat(message()).isEqualTo(SearchReservationsView.NO_MATCH);
        assertThat($(Grid.class).exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Too many matches
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007", scenario = "A3: Too Many Matches", businessRules = {"BR-002", "BR-003"})
    void first_100_matches_are_listed_and_the_receptionist_is_asked_to_narrow_the_search() {
        openSearch();

        // Max Vielbucher has 101 cancelled reservations
        search("Vielbucher", null, null);

        Grid<ReservationSearchResult> grid = grid();
        assertThat(test(grid).size()).isEqualTo(100);
        assertThat(test(grid).getRow(0).reservationCode()).isEqualTo("MANY0001");
        assertThat(test(grid).getRow(99).reservationCode()).isEqualTo("MANY0100");
        assertThat(test(grid).getRow(0).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(message()).isEqualTo(SearchReservationsView.TOO_MANY_MATCHES);

        // A3 step 2: continues at step 4
        test(grid).select(99);
        assertThat($(H1.class).single().getText()).isEqualTo("Reservation MANY0100");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-007")
    void only_a_logged_in_receptionist_can_search_reservations() {
        navigate("reservations", LoginView.class);
        navigate("reservation-details/SEARCH0001", LoginView.class);

        logIn("mark");
        assertThat($(RouterLink.class).withText("Reservations").exists()).isFalse();
        assertThat(navigate("reservations", RouteNotFoundError.class)).isNotNull();
        assertThat(navigate("reservation-details/SEARCH0001", RouteNotFoundError.class)).isNotNull();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openSearch() {
        logIn("rita");
        navigate(SearchReservationsView.class);
    }

    private void search(String guestName, String reservationCode, LocalDate date) {
        Component view = (Component) getCurrentView();
        test($(TextField.class, view).withCaption("Guest name").single()).setValue(guestName == null ? "" : guestName);
        test($(TextField.class, view).withCaption("Reservation code").single())
                .setValue(reservationCode == null ? "" : reservationCode);
        DatePicker datePicker = $(DatePicker.class, view).withCaption("Date").single();
        if (date == null) {
            test(datePicker).clear();
        } else {
            test(datePicker).setValue(date);
        }
        test($(Button.class, view).withText("Search").single()).click();
    }

    @SuppressWarnings("unchecked")
    private Grid<ReservationSearchResult> grid() {
        return (Grid<ReservationSearchResult>) $(Grid.class).single();
    }

    private List<String> codes(Grid<ReservationSearchResult> grid) {
        return IntStream.range(0, test(grid).size()).mapToObj(row -> test(grid).getRow(row).reservationCode())
                .toList();
    }

    private String message() {
        return $(Paragraph.class).withId("search-message").single().getText();
    }

    private String field(String id) {
        return $(Paragraph.class).withId(id).single().getText();
    }

    private List<String> actions() {
        HorizontalLayout actions = $(HorizontalLayout.class).withId("actions").single();
        return $(Button.class, actions).all().stream().map(Button::getText).toList();
    }

    private Result<Record> allReservationAndGuestData() {
        return ctx.select().from(RESERVATION).join(GUEST).on(GUEST.ID.eq(RESERVATION.GUEST_ID))
                .orderBy(RESERVATION.ID).fetch();
    }
}
