package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reservation.ReservationSearchCriteria;
import ch.martinelli.edu.hrs.reservation.ReservationSearchResult;
import ch.martinelli.edu.hrs.reservation.ReservationSearchResults;
import ch.martinelli.edu.hrs.reservation.ReservationSearchService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-007 Search Reservations: the receptionist finds reservations by guest name, reservation code or date and opens
 * one of them. The view changes no data.
 */
@Route(value = "reservations", layout = StaffLayout.class)
@PageTitle("Reservations")
// NFR-009: Only the receptionist can search reservations.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class SearchReservationsView extends VerticalLayout {

    static final String NO_CRITERION = "Enter a guest name, a reservation code or a date.";
    static final String NO_MATCH = "No reservation matches the search.";
    static final String TOO_MANY_MATCHES = "More than " + ReservationSearchService.MAX_RESULTS
            + " reservations match. The first " + ReservationSearchService.MAX_RESULTS
            + " are shown; please narrow the search.";

    private final ReservationSearchService searchService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    // Step 2
    private final TextField guestName = new TextField("Guest name");
    private final TextField reservationCode = new TextField("Reservation code");
    private final DatePicker date = new DatePicker("Date");

    // Step 3
    private final Div message = new Div();
    private final Grid<ReservationSearchResult> grid = new Grid<>();

    public SearchReservationsView(ReservationSearchService searchService) {
        this.searchService = searchService;

        add(new H1("Reservations"));
        add(createSearchForm());

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);

        createGrid();
        add(grid);
    }

    private FormLayout createSearchForm() {
        guestName.setMaxLength(100);
        guestName.setHelperText("Part of the first or last name");
        guestName.setClearButtonVisible(true);
        reservationCode.setMaxLength(20);
        reservationCode.setHelperText("The exact code");
        reservationCode.setClearButtonVisible(true);
        date.setHelperText("A day of the stay");
        date.setClearButtonVisible(true);

        Button searchButton = new Button("Search", _ -> search());
        searchButton.addThemeVariants(ButtonVariant.PRIMARY);
        searchButton.addClickShortcut(Key.ENTER);

        FormLayout form = new FormLayout(guestName, reservationCode, date, searchButton);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("40rem", 4));
        form.setWidthFull();
        return form;
    }

    private void createGrid() {
        grid.addColumn(ReservationSearchResult::reservationCode).setHeader("Reservation code").setAutoWidth(true);
        grid.addColumn(ReservationSearchResult::guestName).setHeader("Guest").setAutoWidth(true);
        grid.addColumn(r -> dateFormatter.format(r.arrivalDate())).setHeader("Arrival").setAutoWidth(true);
        grid.addColumn(r -> dateFormatter.format(r.departureDate())).setHeader("Departure").setAutoWidth(true);
        grid.addColumn(ReservationSearchResult::roomTypeName).setHeader("Room type").setAutoWidth(true);
        grid.addColumn(ReservationSearchResult::numberOfGuests).setHeader("Guests").setAutoWidth(true);
        grid.addColumn(r -> ReservationView.statusText(r.status())).setHeader("Status").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        // Step 4: the receptionist selects a reservation
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                open(event.getValue());
            }
        });
        grid.setVisible(false);
    }

    /**
     * Step 3, A1 to A4.
     */
    private void search() {
        message.removeAll();
        ReservationSearchCriteria criteria = new ReservationSearchCriteria(guestName.getValue(),
                reservationCode.getValue(), date.getValue());
        if (criteria.isEmpty()) {
            // A1: ask for at least one criterion
            grid.setVisible(false);
            message.add(paragraph("search-message", NO_CRITERION, true));
            return;
        }
        ReservationSearchResults results = searchService.search(criteria);
        List<ReservationSearchResult> reservations = results.reservations();
        grid.setItems(reservations);
        grid.setVisible(!reservations.isEmpty());
        if (reservations.isEmpty()) {
            // A2: no matching reservation
            message.add(paragraph("search-message", NO_MATCH, false));
        } else if (results.moreMatches()) {
            // A3: the first 100 matches, ask to narrow the search
            message.add(paragraph("search-message", TOO_MANY_MATCHES, false));
        }
    }

    private void open(ReservationSearchResult reservation) {
        UI.getCurrent().navigate(ReservationView.class, reservation.reservationCode());
    }

    private static Paragraph paragraph(String id, String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId(id);
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        return paragraph;
    }
}
