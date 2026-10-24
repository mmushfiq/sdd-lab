package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.core.ui.HrsBrowserlessTest;
import ch.martinelli.edu.hrs.report.MonthlyRevenue;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static ch.martinelli.edu.hrs.db.Tables.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-020 "View Revenue Report". Test data: V118__uc020_test_data.sql (inactive room type Mansion;
 * Checked Out reservations REVENU0001 Mansion CHF 1000.00 on 15 November 2028, REVENU0002 Mansion CHF 1600.00
 * on 2 December 2028 after a stay from 28 November, REVENU0003 Single Room CHF 120.50 on 31 December 2028 23:30,
 * REVENU0004 and REVENU0005 just before and after November and December 2028; one Cancelled and one No-Show
 * reservation); staff members from V103__uc022_test_data.sql (password Secret-123). The active room types are
 * Single Room, Double Room and Family Room (V100). Today is 1 June 2030.
 */
@WithAnonymousUser
class UC020ViewRevenueReportTest extends HrsBrowserlessTest {

    private static final String ALLOWED_PERIOD =
            "The period can be at most 366 days long and must end today or earlier.";
    private static final String ZERO = "CHF 0.00";

    @TestBean
    Clock clock;

    @Autowired
    DSLContext ctx;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2030-06-01T10:00:00Z"), ZoneId.of("Europe/Zurich"));
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Main success scenario
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-020", businessRules = {"BR-001", "BR-002", "BR-003", "BR-004"})
    void manager_sees_revenue_per_month_and_room_type() {
        int reservations = ctx.fetchCount(RESERVATION);
        logIn("mark");

        // Step 1
        test($(RouterLink.class).withText("Revenue report").single()).click();
        assertThat(getCurrentView()).isInstanceOf(RevenueReportView.class);

        // Steps 2 and 3
        showReport(LocalDate.of(2028, 11, 1), LocalDate.of(2028, 12, 31));
        assertThat($(Paragraph.class).withId("revenue-message").exists()).isFalse();

        // Step 5: one row per month, one column per room type; BR-003: the inactive Mansion has revenue and a
        // column, the inactive Suite and Villa have none and no column
        assertThat(headers()).containsExactly("Month", "Double Room", "Family Room", "Mansion", "Single Room",
                "Total");
        // BR-001: Checked Out only, by time of check-out in the period; Cancelled and No-Show count nothing.
        // BR-002: REVENU0002 counts entirely for December, the month of its check-out.
        assertThat(rows()).containsExactly(
                List.of("November 2028", ZERO, ZERO, "CHF 1,000.00", ZERO, "CHF 1,000.00"),
                List.of("December 2028", ZERO, ZERO, "CHF 1,600.00", "CHF 120.50", "CHF 1,720.50"));
        // Totals per room type and for the whole period
        assertThat(footer()).containsExactly("Total", ZERO, ZERO, "CHF 2,600.00", "CHF 120.50", "CHF 2,720.50");
        assertThat(total()).isEqualTo("Revenue from Nov 1, 2028 to Dec 31, 2028: CHF 2,720.50");

        // Success postcondition: no data is changed
        assertThat(ctx.fetchCount(RESERVATION)).isEqualTo(reservations);
    }

    @Test
    @UseCase(id = "UC-020", businessRules = {"BR-001"})
    void check_out_on_the_first_and_last_date_of_the_period_counts() {
        openReport();

        // REVENU0002 is checked out on 2 December 09:00, REVENU0003 on 31 December 23:30
        showReport(LocalDate.of(2028, 12, 2), LocalDate.of(2028, 12, 31));

        assertThat(rows()).containsExactly(
                List.of("December 2028", ZERO, ZERO, "CHF 1,600.00", "CHF 120.50", "CHF 1,720.50"));
        assertThat(footer()).containsExactly("Total", ZERO, ZERO, "CHF 1,600.00", "CHF 120.50", "CHF 1,720.50");
    }

    @Test
    @UseCase(id = "UC-020", businessRules = {"BR-001", "BR-002"})
    void check_out_outside_the_period_counts_nothing() {
        openReport();

        // REVENU0001 is checked out on 15 November, REVENU0002 on 2 December, although part of its stay lies in
        // the period
        showReport(LocalDate.of(2028, 11, 16), LocalDate.of(2028, 12, 1));

        assertThat(rows()).containsExactly(
                List.of("November 2028", ZERO, ZERO, ZERO, ZERO),
                List.of("December 2028", ZERO, ZERO, ZERO, ZERO));
    }

    @Test
    @UseCase(id = "UC-020", businessRules = {"BR-004"})
    void period_of_366_days_ending_today_is_accepted() {
        openReport();

        showReport(LocalDate.of(2029, 6, 1), LocalDate.of(2030, 6, 1));

        assertThat($(Paragraph.class).withId("revenue-message").exists()).isFalse();
        assertThat(test(grid()).size()).isEqualTo(13);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A1: Invalid Period
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-020", scenario = "A1: Invalid Period")
    void missing_dates_are_marked() {
        openReport();

        test($(Button.class).withText("Show report").single()).click();

        assertThat(firstDate().isInvalid()).isTrue();
        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("revenue").exists()).isFalse();

        // A1 step 2: the use case continues at step 2
        showReport(LocalDate.of(2028, 11, 1), LocalDate.of(2028, 12, 31));
        assertThat($(Paragraph.class).withId("revenue-message").exists()).isFalse();
        assertThat(total()).isEqualTo("Revenue from Nov 1, 2028 to Dec 31, 2028: CHF 2,720.50");
    }

    @Test
    @UseCase(id = "UC-020", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void last_date_before_first_date_is_marked() {
        openReport();

        showReport(LocalDate.of(2028, 12, 1), LocalDate.of(2028, 11, 30));

        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The last date must be on or after the first date");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("revenue").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-020", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void last_date_after_today_is_marked() {
        openReport();
        assertThat(lastDate().getMax()).isEqualTo(LocalDate.of(2030, 6, 1));

        showReport(LocalDate.of(2030, 5, 1), LocalDate.of(2030, 6, 2));

        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The last date must be today or earlier");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("revenue").exists()).isFalse();
    }

    @Test
    @UseCase(id = "UC-020", scenario = "A1: Invalid Period", businessRules = {"BR-004"})
    void period_longer_than_366_days_is_marked() {
        openReport();

        showReport(LocalDate.of(2029, 5, 31), LocalDate.of(2030, 6, 1));

        assertThat(lastDate().isInvalid()).isTrue();
        assertThat(lastDate().getErrorMessage()).isEqualTo("The period can be at most 366 days long");
        assertThat(message()).isEqualTo(ALLOWED_PERIOD);
        assertThat($(Grid.class).withId("revenue").exists()).isFalse();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // A2: No Revenue in the Period
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-020", scenario = "A2: No Revenue in the Period", businessRules = {"BR-003"})
    void period_without_check_outs_shows_zero_for_every_month_and_room_type() {
        openReport();

        showReport(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 3, 31));

        // A2 step 1: 0.00 CHF for every month and room type
        assertThat(headers()).containsExactly("Month", "Double Room", "Family Room", "Single Room", "Total");
        assertThat(rows()).containsExactly(
                List.of("January 2028", ZERO, ZERO, ZERO, ZERO),
                List.of("February 2028", ZERO, ZERO, ZERO, ZERO),
                List.of("March 2028", ZERO, ZERO, ZERO, ZERO));
        // A2 step 2: the totals are displayed as in step 5
        assertThat(footer()).containsExactly("Total", ZERO, ZERO, ZERO, ZERO);
        assertThat(total()).isEqualTo("Revenue from Jan 1, 2028 to Mar 31, 2028: CHF 0.00");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Preconditions
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-020")
    void other_roles_cannot_open_the_revenue_report() {
        // Precondition: the manager is logged in (NFR-009)
        logIn("rita");

        assertThat($(RouterLink.class).withText("Revenue report").exists()).isFalse();
        assertThat(navigate("revenue-report", RouteNotFoundError.class)).isNotNull();
    }

    @Test
    @UseCase(id = "UC-020")
    void anonymous_user_is_sent_to_the_login() {
        navigate("revenue-report", LoginView.class);

        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
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

    private void openReport() {
        logIn("mark");
        navigate(RevenueReportView.class);
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
        return $(Paragraph.class).withId("revenue-message").single().getText();
    }

    private String total() {
        return $(Span.class).withId("revenue-total").single().getText();
    }

    @SuppressWarnings("unchecked")
    private Grid<MonthlyRevenue> grid() {
        return $(Grid.class).withId("revenue").single();
    }

    private List<String> headers() {
        return grid().getColumns().stream().map(Grid.Column::getHeaderText).toList();
    }

    private List<List<String>> rows() {
        Grid<MonthlyRevenue> grid = grid();
        return IntStream.range(0, test(grid).size())
                .mapToObj(row -> IntStream.range(0, grid.getColumns().size())
                        .mapToObj(column -> test(grid).getCellText(row, column)).toList())
                .toList();
    }

    private List<String> footer() {
        Grid<MonthlyRevenue> grid = grid();
        return grid.getColumns().stream()
                .map(column -> grid.getFooterRows().getFirst().getCell(column).getText())
                .toList();
    }
}
