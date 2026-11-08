package ch.martinelli.edu.hrs.report;

import ch.martinelli.edu.hrs.core.ui.MainLayout;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

@Route(value = "manager/reports", layout = MainLayout.class)
@PageTitle("Reports | Hotel Reservation System")
@Menu(title = "Reports", order = 54)
@RolesAllowed("MANAGER")
public class ReportView extends VerticalLayout {

    // shortcut: offers the last 36 months only, add a year field if older reports are needed
    static final int MONTHS_OFFERED = 36;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final transient ReportService reportService;
    private final Span occupancy = value("occupancy");
    private final Span revenue = value("revenue");
    private final Span onlineShare = value("online-share");

    public ReportView(ReportService reportService, Clock clock) {
        this.reportService = reportService;

        var current = YearMonth.now(clock);
        var month = new Select<YearMonth>();
        month.setLabel("Month");
        month.setItems(Stream.iterate(current, m -> m.minusMonths(1)).limit(MONTHS_OFFERED).toList());
        month.setItemLabelGenerator(ReportView::label);
        month.addValueChangeListener(e -> show(e.getValue()));

        var figures = new FormLayout();
        figures.addFormItem(occupancy, "Occupancy");
        figures.addFormItem(revenue, "Revenue");
        figures.addFormItem(onlineShare, "Booked online");
        add(month, figures);
        month.setValue(current);
    }

    static String label(YearMonth month) {
        return MONTH.format(month);
    }

    private void show(YearMonth month) {
        var report = reportService.monthlyReport(month);
        occupancy.setText(percent(report.occupancyPercent()));
        revenue.setText(report.revenue().setScale(2).toPlainString());
        onlineShare.setText(percent(report.onlineSharePercent()));
    }

    private static String percent(BigDecimal value) {
        return value == null ? "–" : value.toPlainString() + "%";
    }

    private static Span value(String id) {
        var span = new Span();
        span.setId(id);
        return span;
    }
}
