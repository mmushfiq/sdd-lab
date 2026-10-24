package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.core.MutableClock;
import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.reception.Arrival;
import ch.martinelli.edu.hrs.reception.Departure;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-008 "View Arrivals and Departures". Test data: V108__uc008_test_data.sql; staff members from
 * V103__uc022_test_data.sql (password Secret-123). Today is 20 July 2031, a date only this test uses; on that date the
 * checked-in reservations of other use cases' test data (e.g. SEARCH0003 from V105) are overdue as well.
 */
@WithAnonymousUser
class UC008ViewArrivalsAndDeparturesTest extends HrsBrowserlessTest {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Europe/Zurich");
    private static final Instant TODAY_NOON = Instant.parse("2031-07-20T10:00:00Z");
    private static final MutableClock CLOCK = new MutableClock(TODAY_NOON, HOTEL_ZONE);

    @TestBean
    Clock clock;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return CLOCK;
    }

    @BeforeEach
    void setToday() {
        CLOCK.setInstant(TODAY_NOON);
    }

    @AfterEach
    void clearSecurityContext() {
        CLOCK.setInstant(TODAY_NOON);
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-001"})
    void todays_arrivals_are_listed_by_guest_last_name() {
        Result<Record> dataBefore = allReservationAndRoomData();

        // Step 1: the arrivals and departures are the receptionist's start screen
        logIn("rita");
        assertThat(getCurrentView()).isInstanceOf(ArrivalsAndDeparturesView.class);

        // Step 2: Confirmed and Checked In arrivals, ordered by last name; the cancelled ARRIVE0003 is not listed
        Grid<Arrival> arrivals = arrivalsGrid();
        assertThat(arrivals.getColumns()).extracting(Grid.Column::getHeaderText).containsExactly(
                "Reservation code", "Guest", "Room type", "Nights", "Guests", "Status");
        assertThat(rows(arrivals)).containsExactly(
                List.of("ARRIVE0002", "Adam Ankunft", "Loft", "1", "1", "Checked In (done)"),
                List.of("ARRIVE0001", "Zora Zeller", "Loft", "3", "2", "Confirmed"));

        // Success postcondition: no reservation or room data is changed
        assertThat(allReservationAndRoomData()).isEqualTo(dataBefore);
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A2: Overdue Departures", businessRules = {"BR-002"})
    void todays_departures_and_overdue_departures_are_listed_by_room_number() {
        logIn("rita");

        // Step 3: Checked In and Checked Out departures plus the overdue ones, ordered by room number; the no-show
        // DEPART0004 is not listed
        Grid<Departure> departures = departuresGrid();
        assertThat(departures.getColumns()).extracting(Grid.Column::getHeaderText).containsExactly(
                "Reservation code", "Guest", "Room", "Total price", "Status");
        // Every checked-in test reservation of other use cases with a departure date before today is overdue on
        // 20 July 2031 as well; this test checks its own reservations and one of the others
        List<List<String>> rows = rows(departures);
        assertThat(rows).filteredOn(row -> row.getFirst().startsWith("DEPART")).containsExactly(
                List.of("DEPART0002", "Bruno Weg", "602", "CHF 320.00", "Checked Out (done)"),
                List.of("DEPART0001", "Doris Abreise", "603", "CHF 480.00", "Checked In"),
                // A2: departure date 19 July, still checked in
                List.of("DEPART0003", "Otto Spaet", "604", "CHF 640.00", "Checked In (overdue)"));
        assertThat(rows).contains(
                List.of("SEARCH0003", "Sophie Suchmann", "401", "CHF 800.00", "Checked In (overdue)"));
        // Ordered by room number
        List<String> roomNumbers = rows.stream().map(row -> row.get(2)).toList();
        assertThat(roomNumbers).isSorted();
    }

    @Test
    @UseCase(id = "UC-008")
    void selecting_an_arrival_shows_the_reservation_with_check_in() {
        logIn("rita");

        // Step 4
        test(arrivalsGrid()).select(1);

        // Step 5: the reservation and the action its status allows
        assertThat(getCurrentView()).isInstanceOf(ReservationView.class);
        assertThat($(H1.class).single().getText()).isEqualTo("Reservation ARRIVE0001");
        assertThat($(Button.class).withText("Check in").exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-008")
    void selecting_a_departure_shows_the_reservation_with_check_out() {
        logIn("rita");

        Grid<Departure> departures = departuresGrid();
        int row = IntStream.range(0, test(departures).size())
                .filter(index -> test(departures).getRow(index).reservationCode().equals("DEPART0001"))
                .findFirst().orElseThrow();
        test(departures).select(row);

        assertThat($(H1.class).single().getText()).isEqualTo("Reservation DEPART0001");
        assertThat($(Paragraph.class).withId("room").single().getText()).isEqualTo("Room: 603");
        assertThat($(Button.class).withText("Check out").exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-008", businessRules = {"BR-003"})
    void today_is_the_date_in_the_hotels_local_time() {
        // 22:30 UTC on 19 July is already 00:30 on 20 July in Zurich
        CLOCK.setInstant(Instant.parse("2031-07-19T22:30:00Z"));

        logIn("rita");

        assertThat(rows(arrivalsGrid())).extracting(row -> row.getFirst()).containsExactly("ARRIVE0002", "ARRIVE0001");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: No arrivals or no departures today
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-008", scenario = "A1: No Arrivals or No Departures Today")
    void empty_lists_are_shown_as_empty_for_today() {
        // On 25 May 2030 no reservation arrives or departs and none is overdue
        CLOCK.setInstant(Instant.parse("2030-05-25T10:00:00Z"));

        logIn("rita");

        assertThat($(Paragraph.class).withId("no-arrivals").single().getText())
                .isEqualTo(ArrivalsAndDeparturesView.NO_ARRIVALS);
        // A1 step 2: continues at step 3
        assertThat($(Paragraph.class).withId("no-departures").single().getText())
                .isEqualTo(ArrivalsAndDeparturesView.NO_DEPARTURES);
        assertThat($(Grid.class).exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-008", scenario = "A1: No Arrivals or No Departures Today")
    void empty_arrivals_do_not_hide_the_departures() {
        // On 19 July 2031 nobody arrives; DEPART0003 departs
        CLOCK.setInstant(Instant.parse("2031-07-19T10:00:00Z"));

        logIn("rita");

        assertThat($(Paragraph.class).withId("no-arrivals").exists()).isTrue();
        assertThat(rows(departuresGrid())).extracting(row -> row.getFirst()).contains("DEPART0003");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A3: Receptionist only reviews the lists
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-008", scenario = "A3: Receptionist Only Reviews the Lists")
    void receptionist_leaves_the_lists_without_selecting_a_reservation() {
        Result<Record> dataBefore = allReservationAndRoomData();
        logIn("rita");

        navigate(SearchReservationsView.class);

        assertThat(allReservationAndRoomData()).isEqualTo(dataBefore);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Precondition: the receptionist is logged in (NFR-009)
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-008")
    void only_a_logged_in_receptionist_can_view_the_arrivals_and_departures() {
        navigate("arrivals-and-departures", LoginView.class);

        logIn("hanna");
        assertThat(navigate("arrivals-and-departures", RouteNotFoundError.class)).isNotNull();
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

    @SuppressWarnings("unchecked")
    private Grid<Arrival> arrivalsGrid() {
        return $(Grid.class).withId("arrivals").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<Departure> departuresGrid() {
        return $(Grid.class).withId("departures").single();
    }

    private List<List<String>> rows(Grid<?> grid) {
        int columns = grid.getColumns().size();
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> IntStream.range(0, columns).mapToObj(column -> test(grid).getCellText(row, column))
                        .toList())
                .toList();
    }

    private Result<Record> allReservationAndRoomData() {
        return ctx.select().from(RESERVATION).leftJoin(ROOM).on(ROOM.ID.eq(RESERVATION.ROOM_ID))
                .orderBy(RESERVATION.ID).fetch();
    }
}
