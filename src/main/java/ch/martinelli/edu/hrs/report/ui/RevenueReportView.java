package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.report.InvalidReportPeriodException;
import ch.martinelli.edu.hrs.report.MonthlyRevenue;
import ch.martinelli.edu.hrs.report.ReportPeriod;
import ch.martinelli.edu.hrs.report.RevenueReport;
import ch.martinelli.edu.hrs.report.RevenueReportService;
import ch.martinelli.edu.hrs.report.RevenueRoomType;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * UC-020 View Revenue Report: the manager sees the revenue per month and per room type for a chosen period.
 */
@Route(value = "revenue-report", layout = StaffLayout.class)
@PageTitle("Revenue Report")
// NFR-009: Only the manager can see the revenue report.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class RevenueReportView extends VerticalLayout {

    static final String ALLOWED_PERIOD = "The period can be at most %d days long and must end today or earlier."
            .formatted(ReportPeriod.MAX_DAYS);

    private final RevenueReportService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final DatePicker firstDate = new DatePicker("First date");
    private final DatePicker lastDate = new DatePicker("Last date");
    private final Binder<PeriodInput> binder = new Binder<>();
    private final PeriodInput input = new PeriodInput();
    private final Div message = new Div();

    private final VerticalLayout result = new VerticalLayout();
    private final Span periodTotal = new Span();
    private final Grid<MonthlyRevenue> grid = new Grid<>();

    public RevenueReportView(RevenueReportService service) {
        this.service = service;
        add(new H1("Revenue Report"));

        createPeriodForm();
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);

        periodTotal.setId("revenue-total");
        periodTotal.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD);
        grid.setId("revenue");
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        result.setPadding(false);
        result.add(periodTotal, grid);
        result.setVisible(false);
        add(result);
    }

    /**
     * Step 2: the first and the last date of the period.
     */
    private void createPeriodForm() {
        LocalDate today = service.today();
        firstDate.setRequiredIndicatorVisible(true);
        firstDate.setMax(today);
        firstDate.setI18n(new DatePicker.DatePickerI18n()
                .setMaxErrorMessage("The first date must be today or earlier"));
        lastDate.setRequiredIndicatorVisible(true);
        lastDate.setMax(today);
        lastDate.setI18n(new DatePicker.DatePickerI18n()
                .setMaxErrorMessage("The last date must be today or earlier"));
        lastDate.setHelperText("Today or earlier; the period can be at most " + ReportPeriod.MAX_DAYS + " days long");

        binder.forField(firstDate)
                .asRequired("Enter the first date")
                .bind(PeriodInput::getFirstDate, PeriodInput::setFirstDate);
        // UC-020 BR-004: The period ends today or earlier, after its first date, and is at most 366 days long.
        binder.forField(lastDate)
                .asRequired("Enter the last date")
                .withValidator(date -> firstDate.getValue() == null || !date.isBefore(firstDate.getValue()),
                        "The last date must be on or after the first date")
                .withValidator(date -> !date.isAfter(service.today()), "The last date must be today or earlier")
                .withValidator(date -> firstDate.getValue() == null
                                || ReportPeriod.days(firstDate.getValue(), date) <= ReportPeriod.MAX_DAYS,
                        "The period can be at most " + ReportPeriod.MAX_DAYS + " days long")
                .bind(PeriodInput::getLastDate, PeriodInput::setLastDate);
        firstDate.addValueChangeListener(_ -> {
            if (lastDate.getValue() != null) {
                binder.validate();
            }
        });
        binder.setBean(input);

        // Step 3
        Button showButton = new Button("Show report", _ -> show());
        showButton.addThemeVariants(ButtonVariant.PRIMARY);

        HorizontalLayout form = new HorizontalLayout(firstDate, lastDate, showButton);
        form.setWrap(true);
        form.setAlignItems(FlexComponent.Alignment.BASELINE);
        add(form);
    }

    /**
     * Steps 3 to 5, A1 and A2.
     */
    private void show() {
        message.removeAll();
        // A1: the invalid dates are marked and the allowed period is explained
        if (!binder.validate().isOk()) {
            result.setVisible(false);
            showError(ALLOWED_PERIOD);
            return;
        }
        RevenueReport report;
        try {
            report = service.report(input.getFirstDate(), input.getLastDate());
        } catch (InvalidReportPeriodException e) {
            result.setVisible(false);
            showError(e.getMessage() + ". " + ALLOWED_PERIOD);
            return;
        }
        showTable(report);
        periodTotal.setText("Revenue from %s to %s: %s".formatted(dateFormatter.format(report.firstDate()),
                dateFormatter.format(report.lastDate()), chf(report.total())));
        result.setVisible(true);
    }

    /**
     * Step 5: one row per month, one column per room type, totals per month, per room type and for the period.
     */
    private void showTable(RevenueReport report) {
        grid.removeAllColumns();
        while (!grid.getFooterRows().isEmpty()) {
            grid.removeFooterRow(grid.getFooterRows().getFirst());
        }
        Grid.Column<MonthlyRevenue> monthColumn = grid.addColumn(month -> monthFormatter.format(month.month()))
                .setHeader("Month").setAutoWidth(true).setFrozen(true);
        FooterRow footer = grid.appendFooterRow();
        footer.getCell(monthColumn).setText("Total");
        for (RevenueRoomType roomType : report.roomTypes()) {
            Grid.Column<MonthlyRevenue> column = grid.addColumn(month -> chf(month.revenue(roomType.id())))
                    .setHeader(roomType.name()).setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
            footer.getCell(column).setText(chf(report.totalOf(roomType.id())));
        }
        Grid.Column<MonthlyRevenue> totalColumn = grid.addColumn(month -> chf(month.total()))
                .setHeader("Total").setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        footer.getCell(totalColumn).setText(chf(report.total()));
        grid.setItems(report.months());
    }

    /**
     * UC-020 BR-001: Revenue in CHF with two decimal places.
     */
    static String chf(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return "CHF " + format.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private void showError(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("revenue-message");
        paragraph.getElement().setAttribute("role", "alert");
        paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        message.removeAll();
        message.add(paragraph);
    }

    private static class PeriodInput {
        private LocalDate firstDate;
        private LocalDate lastDate;

        LocalDate getFirstDate() {
            return firstDate;
        }

        void setFirstDate(LocalDate firstDate) {
            this.firstDate = firstDate;
        }

        LocalDate getLastDate() {
            return lastDate;
        }

        void setLastDate(LocalDate lastDate) {
            this.lastDate = lastDate;
        }
    }
}
