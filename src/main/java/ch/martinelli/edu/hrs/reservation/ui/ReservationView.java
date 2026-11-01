package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

import java.util.Map;
import java.util.stream.Collectors;

@Route(value = ReservationView.ROUTE, layout = StaffLayout.class)
@PageTitle("Reservations")
@RolesAllowed("RECEPTIONIST")
@AccessDeniedErrorRouter
public class ReservationView extends VerticalLayout {

    public static final String ROUTE = "reservations";

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;
    private final Map<Long, String> roomNumbers;
    private final Grid<ReservationRecord> grid = new Grid<>();
    private final TextField search = new TextField("Search");

    public ReservationView(ReservationService reservationService, AvailabilityService availabilityService,
                           RoomService roomService) {
        this.reservationService = reservationService;
        this.availabilityService = availabilityService;
        roomNumbers = roomService.findAllRooms().stream()
                .collect(Collectors.toMap(RoomRecord::getId, RoomRecord::getRoomNumber));

        search.setId("reservation-search");
        search.setPlaceholder("Reservation number or guest name");
        search.setClearButtonVisible(true);
        search.addValueChangeListener(_ -> refresh());

        grid.setId("reservations");
        grid.addColumn(ReservationRecord::getReservationNumber).setHeader("Reservation number");
        grid.addColumn(ReservationRecord::getGuestName).setHeader("Guest name");
        grid.addColumn(r -> roomNumbers.get(r.getRoomId())).setHeader("Room");
        grid.addColumn(ReservationRecord::getArrivalDate).setHeader("Arrival");
        grid.addColumn(ReservationRecord::getDepartureDate).setHeader("Departure");
        grid.addColumn(r -> statusLabel(r.getStatus())).setHeader("Status");
        grid.addItemClickListener(e -> openDialog(e.getItem()));

        Button newReservation = new Button("New reservation", _ -> openDialog(null));
        newReservation.setId("new-reservation");
        newReservation.addThemeVariants(ButtonVariant.PRIMARY);

        add(new H2("Reservations"), new HorizontalLayout(search), grid, newReservation);
        refresh();
    }

    private void openDialog(ReservationRecord reservation) {
        new ReservationDialog(reservationService, availabilityService, roomNumbers, reservation, this::refresh).open();
    }

    private void refresh() {
        grid.setItems(reservationService.findUpcoming(search.getValue()));
    }

    public static String statusLabel(String status) {
        return (status.charAt(0) + status.substring(1).toLowerCase()).replace('_', ' ');
    }
}
