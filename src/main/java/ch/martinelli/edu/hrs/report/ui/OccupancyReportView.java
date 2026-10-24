package ch.martinelli.edu.hrs.report.ui;

import ch.martinelli.edu.hrs.report.DailyOccupancy;
import ch.martinelli.edu.hrs.report.InvalidReportPeriodException;
import ch.martinelli.edu.hrs.report.MonthlyOccupancy;
import ch.martinelli.edu.hrs.report.OccupancyFigures;
import ch.martinelli.edu.hrs.report.OccupancyReport;
import ch.martinelli.edu.hrs.report.OccupancyReportService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Function;

/**
 * UC-019 View Occupancy Report: the manager sees the occupancy rate per day, per month and for the whole period.
 * It is also the start screen of the manager after login (UC-022 step 5).
 */
@Route(value = "occupancy-report", layout = StaffLayout.class)
@PageTitle("Occupancy Report")
// UC-022 BR-003, NFR-009: Only the manager can reach UC-019.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class OccupancyReportView extends VerticalLayout {

    static final String ALLOWED_PERIOD = "The period can be at most %d days long and must end today or earlier."
            .formatted(OccupancyReportService.MAX_PERIOD_DAYS);
    static final String NO_AVAILABLE_ROOMS = "No available rooms";

    private final OccupancyReportService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final DatePicker firstDate = new DatePicker("First date");
    private final DatePicker lastDate = new DatePicker("Last date");
    private final Binder<PeriodInput> binder = new Binder<>();
    private final PeriodInput input = new PeriodInput();
    private final Div message = new Div();

    private final VerticalLayout result = new VerticalLayout();
    private final Span periodRate = new Span();
    private final Span periodNights = new Span();
    private final Grid<MonthlyOccupancy> monthGrid = new Grid<>();
    private final Grid<DailyOccupancy> dayGrid = new Grid<>();

    public OccupancyReportView(OccupancyReportService service) {
        this.service = service;
        add(new H1("Occupancy Report"));

        createPeriodForm();
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);
        createResult();
        add(result);
    }

    /**
     * Step 2: the first and the last date of the period.
     */
    private void createPeriodForm() {
        LocalDate today = service.today();
        firstDate.setRequiredIndicatorVisible(true);
        firstDate.setMax(today);
        firstDate.setI18n(new DatePicker.DatePickerI18n().setMaxErrorMessage("The first date must be today or earlier"));
        lastDate.setRequiredIndicatorVisible(true);
        lastDate.setMax(today);
        lastDate.setI18n(new DatePicker.DatePickerI18n().setMaxErrorMessage("The last date must be today or earlier"));
        lastDate.setHelperText("Today or earlier; the period can be at most " + OccupancyReportService.MAX_PERIOD_DAYS
                + " days long");

        binder.forField(firstDate)
                .asRequired("Enter the first date")
                .bind(PeriodInput::getFirstDate, PeriodInput::setFirstDate);
        // UC-019 BR-004: The period ends today or earlier, after its first date, and is at most 366 days long.
        binder.forField(lastDate)
                .asRequired("Enter the last date")
                .withValidator(date -> firstDate.getValue() == null || !date.isBefore(firstDate.getValue()),
                        "The last date must be on or after the first date")
                .withValidator(date -> !date.isAfter(service.today()), "The last date must be today or earlier")
                .withValidator(date -> firstDate.getValue() == null
                                || OccupancyReportService.periodDays(firstDate.getValue(), date)
                                <= OccupancyReportService.MAX_PERIOD_DAYS,
                        "The period can be at most " + OccupancyReportService.MAX_PERIOD_DAYS + " days long")
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

    private void createResult() {
        periodRate.setId("period-rate");
        periodRate.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD);
        periodNights.setId("period-nights");
        periodNights.addClassNames(LumoUtility.TextColor.SECONDARY);
        VerticalLayout period = new VerticalLayout(periodRate, periodNights);
        period.setPadding(false);
        period.setSpacing(false);

        monthGrid.setId("occupancy-months");
        monthGrid.addColumn(month -> monthFormatter.format(month.month())).setHeader("Month").setAutoWidth(true);
        addFigureColumns(monthGrid, MonthlyOccupancy::figures);
        monthGrid.setAllRowsVisible(true);
        monthGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        dayGrid.setId("occupancy-days");
        dayGrid.addColumn(day -> dateFormatter.format(day.day())).setHeader("Day").setAutoWidth(true);
        addFigureColumns(dayGrid, DailyOccupancy::figures);
        dayGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        result.setPadding(false);
        result.add(new H2("Whole period"), period, new H2("Per month"), monthGrid, new H2("Per day"), dayGrid);
        result.setVisible(false);
    }

    private <T> void addFigureColumns(Grid<T> grid, Function<T, OccupancyFigures> figures) {
        grid.addColumn(item -> figures.apply(item).occupiedRoomNights()).setHeader("Occupied room nights")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.addColumn(item -> figures.apply(item).availableRoomNights()).setHeader("Available room nights")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
        grid.addColumn(item -> rate(figures.apply(item))).setHeader("Occupancy rate")
                .setTextAlign(ColumnTextAlign.END).setAutoWidth(true);
    }

    /**
     * Steps 3 to 5, A1.
     */
    private void show() {
        message.removeAll();
        // A1: the invalid dates are marked and the allowed period is explained
        if (!binder.validate().isOk()) {
            result.setVisible(false);
            showError(ALLOWED_PERIOD);
            return;
        }
        OccupancyReport report;
        try {
            report = service.report(input.getFirstDate(), input.getLastDate());
        } catch (InvalidReportPeriodException e) {
            result.setVisible(false);
            showError(e.getMessage() + ". " + ALLOWED_PERIOD);
            return;
        }
        // Step 5
        periodRate.setText(rate(report.period()));
        periodNights.setText("%d occupied of %d available room nights from %s to %s".formatted(
                report.period().occupiedRoomNights(), report.period().availableRoomNights(),
                dateFormatter.format(report.firstDate()), dateFormatter.format(report.lastDate())));
        monthGrid.setItems(report.months());
        dayGrid.setItems(report.days());
        result.setVisible(true);
    }

    /**
     * A2: no rate without available room nights; the row is marked as without available rooms.
     */
    static String rate(OccupancyFigures figures) {
        return figures.occupancyRate().map(rate -> rate.toPlainString() + " %").orElse(NO_AVAILABLE_ROOMS);
    }

    private void showError(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("occupancy-message");
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
