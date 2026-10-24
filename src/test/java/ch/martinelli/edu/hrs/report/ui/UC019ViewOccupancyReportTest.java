package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.report.DailyOccupancy;
import ch.martinelli.edu.hrs.report.MonthlyOccupancy;
import ch.martinelli.edu.hrs.report.OccupancyFigures;
import ch.martinelli.edu.hrs.security.ui.LoginView;
import ch.martinelli.edu.hrs.usecase.UseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLink;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static ch.martinelli.edu.hrs.db.Tables.ROOM;
import static ch.martinelli.edu.hrs.db.Tables.ROOM_BLOCK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-019 "View Occupancy Report". Test data: V117__uc019_test_data.sql (room type Dormitory with
 * the active rooms G01 to G03 and the inactive room G04; all active rooms blocked on 10 January 2029; G03 blocked
 * on 31 January and 1 February 2029; Checked Out stays OCCUP00001 in G01 from 30 January to 2 February 2029 and
 * OCCUP00002 in G02 for the night of 31 January 2029); staff members from V103__uc022_test_data.sql
 * (password Secret-123). Today is 1 June 2030.
 * <p>
 * The available room nights depend on the number of active rooms of all test data, which the tests read from the
 * database; no other test data has reservations or room blocks in January or February 2029.
 */
@WithAnonymousUser
class UC019ViewOccupancyReportTest extends HrsBrowserlessTest {

    private static final LocalDate FIRST_DATE = LocalDate.of(2029, 1, 1);
    private static final LocalDate LAST_DATE = LocalDate.of(2029, 2, 28);
    private static final String ALLOWED_PERIOD =
            "The period can be at most 366 days long and must end today or earlier.";

    @TestBean
    Clock clock;

    @Autowired
    DSLContext ctx;

    /** The active rooms of all test data, each one available room night per unblocked day (BR-003). */
    private long rooms;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @BeforeEach
    void countActiveRooms() {
        rooms = ctx.fetchCount(ROOM, ROOM.ACTIVE.isTrue());
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-019", businessRules = {"BR-001", "BR-002", "BR-003", "BR-004"})
    void manager_sees_occupancy_per_day_month_and_period() {
        int reservations = ctx.fetchCount(RESERVATION);
        int blocks = ctx.fetchCount(ROOM_BLOCK);
        logIn("mark");

        // Step 1: the occupancy report is the manager's start screen and in the navigation
        assertThat(getCurrentView()).isInstanceOf(OccupancyReportView.class);
        test($(RouterLink.class).withText("Occupancy report").single()).click();
        assertThat(getCurrentView()).isInstanceOf(OccupancyReportView.class);

        // Steps 2 and 3
        showReport(FIRST_DATE, LAST_DATE);
        assertThat($(Paragraph.class).withId("occupancy-message").exists()).isFalse();

        // Step 5: per day; BR-002 counts Checked In and Checked Out stays from arrival to the night before departure,
        // BR-003 counts active rooms that are not blocked
        Grid<DailyOccupancy> days = dayGrid();
        assertThat(days.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Day", "Occupied room nights", "Available room nights", "Occupancy rate");
        assertThat(test(days).size()).isEqualTo(59);
        assertThat(dayRow("Jan 1, 2029")).containsExactly("Jan 1, 2029", "0", str(rooms), rate(0, rooms));
        assertThat(dayRow("Jan 29, 2029")).containsExactly("Jan 29, 2029", "0", str(rooms), rate(0, rooms));
        assertThat(dayRow("Jan 30, 2029")).containsExactly("Jan 30, 2029", "1", str(rooms), rate(1, rooms));
        // G03 is blocked; the block of the inactive room G04 does not count; Cancelled and No-Show do not count
        assertThat(dayRow("Jan 31, 2029")).containsExactly("Jan 31, 2029", "2", str(rooms - 1), rate(2, rooms - 1));
        assertThat(dayRow("Feb 1, 2029")).containsExactly("Feb 1, 2029", "1", str(rooms - 1), rate(1, rooms - 1));
        // The departure day of OCCUP00001 is no occupied night
        assertThat(dayRow("Feb 2, 2029")).containsExactly("Feb 2, 2029", "0", str(rooms), rate(0, rooms));
        assertThat(dayRow("Feb 28, 2029")).containsExactly("Feb 28, 2029", "0", str(rooms), rate(0, rooms));

        // Per month; BR-001: from the summed room nights, not the average of the daily rates
        long januaryAvailable = 31 * rooms - rooms - 1;
        long februaryAvailable = 28 * rooms - 1;
        assertThat(monthRows()).containsExactly(
                List.of("January 2029", "3", str(januaryAvailable), rate(3, januaryAvailable)),
                List.of("February 2029", "1", str(februaryAvailable), rate(1, februaryAvailable)));

        // The whole period
        long periodAvailable = januaryAvailable + februaryAvailable;
        assertThat($(Span.class).withId("period-rate").single().getText()).isEqualTo(rate(4, periodAvailable));
        assertThat($(Span.class).withId("period-nights").single().getText()).isEqualTo(
                "4 occupied of %d available room nights from Jan 1, 2029 to Feb 28, 2029".formatted(periodAvailable));

        // Success postcondition: no data is changed
        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(reservations);
        assertThat(ctx.fetchCount(ROOM_BLOCK)).isEqualTo(blocks);
    }

    @Test
    @UseCase(id = "UC-019", businessRules = {"BR-004"})
    void month_partly_in_the_period_counts_only_its_days_in_the_period() {
        openReport();

        showReport(LocalDate.of(2029, 1, 30), LocalDate.of(2029, 2, 2));

        assertThat(test(dayGrid()).size()).isEqualTo(4);
        assertThat(monthRows()).containsExactly(
                List.of("January 2029", "3", str(2 * rooms - 1), rate(3, 2 * rooms - 1)),
                List.of("February 2029", "1", str(2 * rooms - 1), rate(1, 2 * rooms - 1)));
        assertThat($(Span.class).withId("period-rate").single().getText()).isEqualTo(rate(4, 4 * rooms - 2));
    }

    @Test
    @UseCase(id = "UC-019", businessRules = {"BR-004"})
    void period_of_366_days_ending_today_is_accepted() {
        openReport();

        showReport(LocalDate.of(2029, 6, 1), LocalDate.of(2030, 6, 1));

        assertThat($(Paragraph.class).withId("occupancy-message").exists()).isFalse();
        assertThat(test(dayGrid()).size()).isEqualTo(366);
        assertThat(monthRows()).hasSize(13);
    }

    @Test
    @UseCase(id = "UC-019", businessRules = {"BR-001"})
    void occupancy_rate_is_rounded_to_one_decimal_place() {
        assertThat(OccupancyReportView.rate(new OccupancyFigures(1, 3)))
                .isEqualTo("33.3 %");
        assertThat(OccupancyReportView.rate(new OccupancyFigures(2, 3)))
                .isEqualTo("66.7 %");
        assertThat(OccupancyReportView.rate(new OccupancyFigures(1, 1)))
                .isEqualTo("100.0 %");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid Period
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-019", scenario = "A1: Invalid Period")
    void missing_dates_are_marked() {
        openReport();

        test($(Button.class).withText("Show report").single()).click();

        assertThat(firstDate().isInvalid()).isTrue();
        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("occupancy-days").exists()).isFalse();

        // A1 step 2: the use case continues at step 2
        showReport(FIRST_DATE, LAST_DATE);
        assertThat($(Paragraph.class).withId("occupancy-message").exists()).isFalse();
        assertThat(test(dayGrid()).size()).isEqualTo(59);
    }

    @Test
    @UseCase(id = "UC-019", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void last_date_before_first_date_is_marked() {
        openReport();

        showReport(LocalDate.of(2029, 2, 1), LocalDate.of(2029, 1, 31));

        assertThat(firstDate().isInvalid()).isFalse();
        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The last date must be on or after the first date");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("occupancy-days").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-019", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void last_date_after_today_is_marked() {
        openReport();
        assertThat(lastDate().getMax()).isEqualTo(LocalDate.of(2030, 6, 1));

        showReport(LocalDate.of(2030, 5, 1), LocalDate.of(2030, 6, 2));

        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The last date must be today or earlier");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("occupancy-days").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-019", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void period_longer_than_366_days_is_marked() {
        openReport();

        showReport(LocalDate.of(2029, 5, 31), LocalDate.of(2030, 6, 1));

        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The period can be at most 366 days long");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("occupancy-days").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: No Available Room Nights
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-019", scenario = "A2: No Available Room Nights", businessRules = {"BR-001", "BR-003"})
    void day_without_available_rooms_shows_no_rate() {
        openReport();

        showReport(LocalDate.of(2029, 1, 9), LocalDate.of(2029, 1, 11));

        // A2 step 1: no rate, marked as without available rooms
        assertThat(dayRow("Jan 10, 2029")).containsExactly("Jan 10, 2029", "0", "0", "No available rooms");
        assertThat(dayRow("Jan 9, 2029")).containsExactly("Jan 9, 2029", "0", str(rooms), rate(0, rooms));
        // A2 step 2: the month and the period are still displayed, from the other days' room nights
        assertThat(monthRows()).containsExactly(List.of("January 2029", "0", str(2 * rooms), rate(0, 2 * rooms)));
        assertThat($(Span.class).withId("period-rate").single().getText()).isEqualTo(rate(0, 2 * rooms));
    }

    @Test
    @UseCase(id = "UC-019", scenario = "A2: No Available Room Nights", businessRules = {"BR-001"})
    void period_without_available_rooms_shows_no_rate() {
        openReport();

        showReport(LocalDate.of(2029, 1, 10), LocalDate.of(2029, 1, 10));

        assertThat(monthRows()).containsExactly(List.of("January 2029", "0", "0", "No available rooms"));
        assertThat($(Span.class).withId("period-rate").single().getText()).isEqualTo("No available rooms");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Preconditions
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-019")
    void other_roles_cannot_open_the_occupancy_report() {
        // Precondition: the manager is logged in (NFR-009)
        logIn("rita");

        assertThat($(RouterLink.class).withText("Occupancy report").exists()).isFalse();
        assertThat(navigate("occupancy-report", RouteNotFoundError.class)).isNotNull();
    }

    @Test
    @UseCase(id = "UC-019")
    void anonymous_user_is_sent_to_the_login() {
        navigate("occupancy-report", LoginView.class);

        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * BR-001: occupied divided by available room nights, in percent, rounded to one decimal place.
     */
    private static String rate(long occupied, long available) {
        return BigDecimal.valueOf(occupied * 100).divide(BigDecimal.valueOf(available), 1, RoundingMode.HALF_UP)
                .toPlainString() + " %";
    }

    private static String str(long value) {
        return String.valueOf(value);
    }

    private void logIn(String username) {
        navigate(LoginView.class);
        test($(TextField.class).withCaption("Username").single()).setValue(username);
        test($(PasswordField.class).withCaption("Password").single()).setValue("Secret-123");
        test($(Button.class).withText("Log in").single()).click();
    }

    private void openReport() {
        logIn("mark");
        navigate(OccupancyReportView.class);
    }

    private void showReport(LocalDate first, LocalDate last) {
        firstDate().setValue(first);
        lastDate().setValue(last);
        test($(Button.class).withText("Show report").single()).click();
    }

    private DatePicker firstDate() {
        return $(DatePicker.class).withCaption("First date").single();
    }

    private DatePicker lastDate() {
        return $(DatePicker.class).withCaption("Last date").single();
    }

    private String message() {
        return $(Paragraph.class).withId("occupancy-message").single().getText();
    }

    @SuppressWarnings("unchecked")
    private Grid<DailyOccupancy> dayGrid() {
        return $(Grid.class).withId("occupancy-days").single();
    }

    @SuppressWarnings("unchecked")
    private Grid<MonthlyOccupancy> monthGrid() {
        return $(Grid.class).withId("occupancy-months").single();
    }

    private List<String> dayRow(String day) {
        Grid<DailyOccupancy> grid = dayGrid();
        return IntStream.range(0, test(grid).size())
                .filter(index -> test(grid).getCellText(index, 0).equals(day))
                .mapToObj(index -> cells(grid, index))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row for " + day));
    }

    private List<List<String>> monthRows() {
        Grid<MonthlyOccupancy> grid = monthGrid();
        return IntStream.range(0, test(grid).size()).mapToObj(index -> cells(grid, index)).toList();
    }

    private <T> List<String> cells(Grid<T> grid, int row) {
        return IntStream.range(0, grid.getColumns().size())
                .mapToObj(column -> test(grid).getCellText(row, column)).toList();
    }
}
