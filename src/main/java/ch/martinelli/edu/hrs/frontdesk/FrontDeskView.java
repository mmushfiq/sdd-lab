package ch.martinelli.edu.hrs.frontdesk;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.RoomStatus;
import ch.martinelli.edu.hrs.reservation.Stay;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * Today's arrivals and departures with check-in and check-out, and the room overview (User Story 4).
 * First in the reception menu, so a Receptionist lands here after login (contracts/ui-routes.md).
 */
@Route(value = "reception/front-desk", layout = MainLayout.class)
@PageTitle("Front desk | Hotel Reservation System")
@Menu(title = "Front desk", order = 10)
@RolesAllowed("RECEPTIONIST")
public class FrontDeskView extends VerticalLayout implements AfterNavigationObserver {

    private final transient ReservationService reservationService;
    private final transient Clock clock;
    private final Grid<Stay> arrivals = stayGrid("arrivals", "Departure");
    private final Grid<Stay> departures = stayGrid("departures", "Arrival");
    private final Grid<RoomStatus> overview = new Grid<>();

    public FrontDeskView(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;

        overview.setId("overview");
        overview.addColumn(RoomStatus::roomNumber).setHeader("Room");
        overview.addColumn(RoomStatus::roomTypeName).setHeader("Room type");
        overview.addColumn(r -> label(r.occupancy().name())).setHeader("Today");
        overview.addColumn(r -> label(r.cleaningStatus())).setHeader("Cleaning");

        add(new H2("Arrivals today"), arrivals,
                actionButton("Check in", arrivals, stay -> {
                    reservationService.checkIn(stay.reservationId());
                    return stay.guestName() + " checked in to room " + stay.roomNumber() + ".";
                }),
                new H2("Departures today"), departures,
                actionButton("Check out", departures, stay -> {
                    reservationService.checkOut(stay.reservationId());
                    return stay.guestName() + " checked out of room " + stay.roomNumber() + ".";
                }),
                new H2("Room overview"), overview);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Also on re-opening from the menu, which reuses this view
        refresh();
    }

    private Button actionButton(String text, Grid<Stay> grid, SerializableFunction<Stay, String> action) {
        var button = new Button(text, _ -> {
            try {
                var message = action.apply(grid.asSingleSelect().getValue());
                Notification.show(message, 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
            refresh();
        });
        button.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> button.setEnabled(e.getValue() != null));
        return button;
    }

    private void refresh() {
        var today = LocalDate.now(clock);
        arrivals.setItems(reservationService.findArrivals(today));
        departures.setItems(reservationService.findDepartures(today));
        overview.setItems(reservationService.roomOverview(today));
    }

    private static Grid<Stay> stayGrid(String id, String otherDateHeader) {
        var grid = new Grid<Stay>();
        grid.setId(id);
        grid.setAllRowsVisible(true);
        grid.addColumn(Stay::reservationNumber).setHeader("Reservation");
        grid.addColumn(Stay::guestName).setHeader("Guest");
        grid.addColumn(Stay::roomNumber).setHeader("Room");
        grid.addColumn(Stay::numberOfGuests).setHeader("Guests");
        grid.addColumn(s -> "Arrival".equals(otherDateHeader) ? s.arrival() : s.departure())
                .setHeader(otherDateHeader);
        return grid;
    }

    /**
     * FREE → Free, DIRTY → Dirty.
     */
    private static String label(String constant) {
        return constant.charAt(0) + constant.substring(1).toLowerCase();
    }
}
