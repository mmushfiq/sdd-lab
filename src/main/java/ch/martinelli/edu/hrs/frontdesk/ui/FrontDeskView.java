package ch.martinelli.edu.hrs.frontdesk.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService.RoomOverview;
import ch.martinelli.edu.hrs.frontdesk.FrontDeskService.RoomState;
import ch.martinelli.edu.hrs.housekeeping.HousekeepingService;
import ch.martinelli.edu.hrs.reservation.ReservationException;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

/**
 * Room overview for reception with check-in and check-out.
 */
@Route(value = FrontDeskView.ROUTE, layout = StaffLayout.class)
@PageTitle("Front desk")
@RolesAllowed("RECEPTIONIST")
@AccessDeniedErrorRouter
public class FrontDeskView extends VerticalLayout {

    public static final String ROUTE = "front-desk";

    private final FrontDeskService frontDeskService;
    private final Grid<RoomOverview> grid = new Grid<>();

    public FrontDeskView(FrontDeskService frontDeskService) {
        this.frontDeskService = frontDeskService;

        grid.setId("front-desk-rooms");
        grid.addColumn(RoomOverview::roomNumber).setHeader("Room");
        grid.addColumn(RoomOverview::roomTypeName).setHeader("Room type");
        grid.addComponentColumn(r -> badge(r.state().label(), switch (r.state()) {
            case FREE -> "success";
            case ARRIVAL_TODAY -> "contrast";
            case OCCUPIED -> "";
        }, "state-" + r.roomNumber())).setHeader("State");
        grid.addComponentColumn(r -> {
            boolean clean = HousekeepingService.CLEAN.equals(r.cleaningStatus());
            return badge(clean ? "Clean" : "Dirty", clean ? "success" : "error", "cleaning-" + r.roomNumber());
        }).setHeader("Cleaning");
        grid.addColumn(RoomOverview::guestName).setHeader("Guest");
        grid.addColumn(RoomOverview::departureDate).setHeader("Departure");
        grid.addComponentColumn(this::action);

        add(new H2("Front desk"), grid);
        refresh();
    }

    private static Span badge(String text, String variant, String id) {
        Span badge = new Span(text);
        badge.setId(id);
        badge.getElement().getThemeList().add(("badge " + variant).strip());
        return badge;
    }

    private Component action(RoomOverview room) {
        if (room.state() == RoomState.ARRIVAL_TODAY) {
            return actionButton("Check in", "check-in-" + room.roomNumber(),
                    "Check in " + room.guestName() + " to room " + room.roomNumber() + "?",
                    () -> frontDeskService.checkIn(room.reservationId()));
        }
        if (room.state() == RoomState.OCCUPIED) {
            return actionButton("Check out", "check-out-" + room.roomNumber(),
                    "Check out " + room.guestName() + " from room " + room.roomNumber() + "?",
                    () -> frontDeskService.checkOut(room.reservationId()));
        }
        return new Span();
    }

    private Button actionButton(String label, String id, String question, Runnable action) {
        Button button = new Button(label, _ -> {
            ConfirmDialog confirm = new ConfirmDialog(label, question, label, _ -> run(action));
            confirm.setCancelable(true);
            confirm.open();
        });
        button.setId(id);
        return button;
    }

    private void run(Runnable action) {
        try {
            action.run();
        } catch (ReservationException e) {
            Notification.show(e.getMessage()).addThemeVariants(NotificationVariant.ERROR);
        }
        refresh();
    }

    private void refresh() {
        grid.setItems(frontDeskService.roomOverview());
    }
}
