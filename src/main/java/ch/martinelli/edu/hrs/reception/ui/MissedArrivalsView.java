package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reception.MissedArrival;
import ch.martinelli.edu.hrs.reception.NoShowNotPossibleException;
import ch.martinelli.edu.hrs.reception.NoShowService;
import ch.martinelli.edu.hrs.reservation.ReservationSearchService;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-012 Record No-Show: the receptionist reviews the missed arrivals and records a no-show for a guest who did not
 * arrive.
 */
@Route(value = "missed-arrivals", layout = StaffLayout.class)
@PageTitle("Missed Arrivals")
// NFR-009: Only the receptionist can record no-shows.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class MissedArrivalsView extends VerticalLayout {

    static final String NO_MISSED_ARRIVALS = "There are no missed arrivals.";
    static final String CANNOT_BE_CHECKED_IN = "The reservation cannot be checked in afterwards.";
    static final String RECORDED = "Reservation %s is recorded as a no-show.";
    static final String NOT_POSSIBLE = "A no-show cannot be recorded for reservation %s. Its status is %s.";

    private final NoShowService noShowService;
    private final ReservationSearchService searchService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Div message = new Div();
    private final Div list = new Div();
    private final HorizontalLayout actions = new HorizontalLayout();

    private MissedArrival selected;

    public MissedArrivalsView(NoShowService noShowService, ReservationSearchService searchService) {
        this.noShowService = noShowService;
        this.searchService = searchService;
        add(new H1("Missed Arrivals"));
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        list.setWidthFull();

        // Step 4
        Button recordButton = new Button("Record no-show", _ -> requestNoShow());
        recordButton.addThemeVariants(ButtonVariant.ERROR);
        // A2: the guest is still coming; the receptionist changes the arrival date instead (UC-005)
        Button changeButton = new Button("Change reservation",
                _ -> UI.getCurrent().navigate(ChangeReservationView.class, selected.reservationCode()));
        actions.add(recordButton, changeButton);
        actions.setVisible(false);

        add(message, list, actions);
        showList();
    }

    /**
     * Step 2, A1.
     */
    private void showList() {
        list.removeAll();
        selected = null;
        actions.setVisible(false);
        List<MissedArrival> missedArrivals = noShowService.missedArrivals();
        if (missedArrivals.isEmpty()) {
            // A1
            Paragraph none = new Paragraph(NO_MISSED_ARRIVALS);
            none.setId("no-missed-arrivals");
            list.add(none);
            return;
        }
        Grid<MissedArrival> grid = new Grid<>();
        grid.setId("missed-arrivals");
        grid.addColumn(MissedArrival::reservationCode).setHeader("Reservation code").setAutoWidth(true);
        grid.addColumn(MissedArrival::guestName).setHeader("Guest").setAutoWidth(true);
        grid.addColumn(MissedArrival::guestPhone).setHeader("Phone number").setAutoWidth(true);
        grid.addColumn(arrival -> dateFormatter.format(arrival.arrivalDate())).setHeader("Arrival").setAutoWidth(true);
        grid.addColumn(arrival -> dateFormatter.format(arrival.departureDate())).setHeader("Departure")
                .setAutoWidth(true);
        grid.addColumn(MissedArrival::roomTypeName).setHeader("Room type").setAutoWidth(true);
        grid.setItems(missedArrivals);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        // Step 3
        grid.asSingleSelect().addValueChangeListener(event -> {
            selected = event.getValue();
            actions.setVisible(selected != null);
        });
        list.add(grid);
    }

    /**
     * Step 5, A3.
     */
    private void requestNoShow() {
        if (selected == null) {
            return;
        }
        MissedArrival reservation = selected;
        message.removeAll();
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Record reservation " + reservation.reservationCode() + " as a no-show?");
        dialog.setText(CANNOT_BE_CHECKED_IN);
        dialog.setConfirmText("Record no-show");
        dialog.setConfirmButtonTheme("error primary");
        // A3: the reservation stays unchanged; the use case continues at step 2
        dialog.setCancelable(true);
        dialog.setCancelText("Keep reservation");
        dialog.addConfirmListener(_ -> confirmNoShow(reservation));
        dialog.open();
    }

    /**
     * Steps 6 to 9, A4.
     */
    private void confirmNoShow(MissedArrival reservation) {
        try {
            noShowService.recordNoShow(reservation.reservationCode());
            message.add(paragraph(RECORDED.formatted(reservation.reservationCode()), false));
        } catch (NoShowNotPossibleException _) {
            // A4: refuse and display the current status; the use case continues at step 2
            String status = searchService.findReservation(reservation.reservationCode())
                    .map(current -> ReservationView.statusText(current.status())).orElse("unknown");
            message.add(paragraph(NOT_POSSIBLE.formatted(reservation.reservationCode(), status), true));
        }
        showList();
    }

    private static Paragraph paragraph(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("no-show-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        return paragraph;
    }
}
