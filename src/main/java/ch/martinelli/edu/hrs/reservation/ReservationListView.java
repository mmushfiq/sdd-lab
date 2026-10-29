package ch.martinelli.edu.hrs.reservation;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Reservations")
@Route("reservations")
public class ReservationListView extends VerticalLayout {

    private final ReservationService reservationService;

    private final TextField bookingNumber = new TextField("Booking number");
    private final TextField guestName = new TextField("Guest name");
    private final DatePicker from = new DatePicker("From");
    private final DatePicker to = new DatePicker("To");
    private final Checkbox showCancelled = new Checkbox("Show cancelled");
    private final Grid<ReservationListItem> grid = new Grid<>();
    private final Span noReservations = new Span("No reservations found");

    public ReservationListView(ReservationService reservationService) {
        this.reservationService = reservationService;
        setSizeFull();

        bookingNumber.setValueChangeMode(ValueChangeMode.LAZY);
        guestName.setValueChangeMode(ValueChangeMode.LAZY);
        bookingNumber.addValueChangeListener(_ -> refresh());
        guestName.addValueChangeListener(_ -> refresh());
        from.addValueChangeListener(_ -> refresh());
        to.addValueChangeListener(_ -> refresh());
        showCancelled.addValueChangeListener(_ -> refresh());
        var filters = new HorizontalLayout(bookingNumber, guestName, from, to, showCancelled);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        grid.addColumn(ReservationListItem::bookingNumber).setHeader("Booking number");
        grid.addColumn(ReservationListItem::guestName).setHeader("Guest");
        grid.addColumn(ReservationListItem::roomNumber).setHeader("Room");
        grid.addColumn(ReservationListItem::arrival).setHeader("Arrival");
        grid.addColumn(ReservationListItem::departure).setHeader("Departure");
        grid.addColumn(ReservationListItem::numberOfGuests).setHeader("Guests");
        grid.addColumn(ReservationListItem::totalPrice).setHeader("Total (CHF)");
        grid.addColumn(ReservationListItem::status).setHeader("Status");

        add(filters, noReservations, grid);
        refresh();
    }

    private void refresh() {
        var reservations = reservationService.findReservations(new ReservationFilter(bookingNumber.getValue(),
                guestName.getValue(), from.getValue(), to.getValue(), showCancelled.getValue()));
        grid.setItems(reservations);
        noReservations.setVisible(reservations.isEmpty());
    }
}
