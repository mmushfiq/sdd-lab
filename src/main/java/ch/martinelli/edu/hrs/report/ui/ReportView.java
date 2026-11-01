package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.report.ReportService;
import ch.martinelli.edu.hrs.report.ReportService.MonthFigures;
import ch.martinelli.edu.hrs.report.ReportService.YearReport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Monthly occupancy and revenue of one year for the manager, with the year total in the footer.
 */
@Route(value = ReportView.ROUTE, layout = StaffLayout.class)
@PageTitle("Reports")
@RolesAllowed("MANAGER")
@AccessDeniedErrorRouter
public class ReportView extends VerticalLayout {

    public static final String ROUTE = "reports";

    private final ReportService reportService;
    private final Grid<MonthFigures> grid = new Grid<>();
    private final Grid.Column<MonthFigures> monthColumn;
    private final Grid.Column<MonthFigures> nightsColumn;
    private final Grid.Column<MonthFigures> occupancyColumn;
    private final Grid.Column<MonthFigures> revenueColumn;

    public ReportView(ReportService reportService, Clock clock) {
        this.reportService = reportService;
        int currentYear = LocalDate.now(clock).getYear();

        Select<Integer> year = new Select<>();
        year.setId("report-year");
        year.setLabel("Year");
        int firstYear = Math.min(reportService.firstYear().orElse(currentYear), currentYear);
        year.setItems(IntStream.rangeClosed(firstYear, currentYear + 1).boxed().toList());

        grid.setId("report");
        grid.setAllRowsVisible(true);
        monthColumn = grid.addColumn(m -> m.month().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                .setHeader("Month");
        nightsColumn = grid.addColumn(MonthFigures::roomNights).setHeader("Room-nights")
                .setTextAlign(ColumnTextAlign.END);
        occupancyColumn = grid.addColumn(m -> percent(m.occupancyPercent())).setHeader("Occupancy")
                .setTextAlign(ColumnTextAlign.END);
        revenueColumn = grid.addColumn(m -> chf(m.revenue())).setHeader("Revenue")
                .setTextAlign(ColumnTextAlign.END);
        grid.appendFooterRow();

        year.addValueChangeListener(e -> show(e.getValue()));
        year.setValue(currentYear);

        add(new H2("Reports"), year, grid);
    }

    private void show(int year) {
        YearReport report = reportService.yearReport(year);
        grid.setItems(report.months());
        MonthFigures total = report.total();
        monthColumn.setFooter("Total " + year);
        nightsColumn.setFooter(String.valueOf(total.roomNights()));
        occupancyColumn.setFooter(percent(total.occupancyPercent()));
        revenueColumn.setFooter(chf(total.revenue()));
    }

    static String percent(BigDecimal value) {
        return value + " %";
    }

    static String chf(BigDecimal value) {
        return "CHF " + value;
    }
}
