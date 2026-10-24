package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reception.Arrival;
import ch.martinelli.edu.hrs.reception.ArrivalsAndDeparturesService;
import ch.martinelli.edu.hrs.reception.Departure;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.function.Function;

/**
 * UC-008 View Arrivals and Departures: today's arrivals and departures, the start screen of the receptionist after
 * login (UC-022 step 5). Selecting a reservation opens it (step 5). The view changes no data.
 */
@Route(value = "arrivals-and-departures", layout = StaffLayout.class)
@PageTitle("Arrivals and Departures")
// UC-022 BR-003: Only the receptionist can reach UC-008.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class ArrivalsAndDeparturesView extends VerticalLayout {

    static final String NO_ARRIVALS = "No arrivals today.";
    static final String NO_DEPARTURES = "No departures today.";

    public ArrivalsAndDeparturesView(ArrivalsAndDeparturesService service) {
        add(new H1("Arrivals and Departures"));
        add(new Paragraph(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(service.today())));

        // Step 2, A1
        add(new H2("Arrivals"));
        List<Arrival> arrivals = service.arrivals();
        if (arrivals.isEmpty()) {
            add(empty("no-arrivals", NO_ARRIVALS));
        } else {
            add(arrivalsGrid(arrivals));
        }

        // Step 3, A1, A2
        add(new H2("Departures"));
        List<Departure> departures = service.departures();
        if (departures.isEmpty()) {
            add(empty("no-departures", NO_DEPARTURES));
        } else {
            add(departuresGrid(departures));
        }
    }

    private static Grid<Arrival> arrivalsGrid(List<Arrival> arrivals) {
        Grid<Arrival> grid = new Grid<>();
        grid.setId("arrivals");
        grid.addColumn(Arrival::reservationCode).setHeader("Reservation code").setAutoWidth(true);
        grid.addColumn(Arrival::guestName).setHeader("Guest").setAutoWidth(true);
        grid.addColumn(Arrival::roomTypeName).setHeader("Room type").setAutoWidth(true);
        grid.addColumn(Arrival::nights).setHeader("Nights").setAutoWidth(true);
        grid.addColumn(Arrival::numberOfGuests).setHeader("Guests").setAutoWidth(true);
        // UC-008 BR-001: Checked-in arrivals are marked as done.
        grid.addColumn(arrival -> ReservationView.statusText(arrival.status()) + (arrival.done() ? " (done)" : ""))
                .setHeader("Status").setAutoWidth(true);
        configure(grid, arrivals, Arrival::reservationCode);
        return grid;
    }

    private static Grid<Departure> departuresGrid(List<Departure> departures) {
        Grid<Departure> grid = new Grid<>();
        grid.setId("departures");
        grid.addColumn(Departure::reservationCode).setHeader("Reservation code").setAutoWidth(true);
        grid.addColumn(Departure::guestName).setHeader("Guest").setAutoWidth(true);
        grid.addColumn(Departure::roomNumber).setHeader("Room").setAutoWidth(true);
        grid.addColumn(departure -> chf(departure.totalPrice())).setHeader("Total price").setAutoWidth(true);
        // UC-008 BR-002: Checked-out departures are marked as done; A2: overdue ones are marked as overdue.
        grid.addColumn(departure -> ReservationView.statusText(departure.status())
                        + (departure.done() ? " (done)" : "") + (departure.overdue() ? " (overdue)" : ""))
                .setHeader("Status").setAutoWidth(true);
        configure(grid, departures, Departure::reservationCode);
        return grid;
    }

    private static <T> void configure(Grid<T> grid, List<T> items,
                                      Function<T, String> reservationCode) {
        grid.setItems(items);
        grid.setAllRowsVisible(true);
        grid.setWidthFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        // Step 4: selecting a reservation opens it (step 5)
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(ReservationView.class, reservationCode.apply(event.getValue()));
            }
        });
    }

    private static Paragraph empty(String id, String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId(id);
        return paragraph;
    }

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }
}
