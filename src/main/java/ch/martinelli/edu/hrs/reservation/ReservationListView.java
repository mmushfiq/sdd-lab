package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.roomcatalog.RoomCatalogService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.function.Consumer;

@PageTitle("Reservations")
@Route("reservations")
public class ReservationListView extends VerticalLayout {

    private final ReservationService reservationService;
    private final RoomCatalogService roomCatalogService;

    private final TextField bookingNumber = new TextField("Booking number");
    private final TextField guestName = new TextField("Guest name");
    private final DatePicker from = new DatePicker("From");
    private final DatePicker to = new DatePicker("To");
    private final Checkbox showCancelled = new Checkbox("Show cancelled");
    private final Grid<ReservationListItem> grid = new Grid<>();
    private final Span noReservations = new Span("No reservations found");
    private final Button change = new Button("Change", _ -> openChange());
    private final Button cancel = new Button("Cancel reservation", _ -> confirmCancel());
    private final Button anonymize = new Button("Anonymize", _ -> confirmAnonymize());

    public ReservationListView(ReservationService reservationService, RoomCatalogService roomCatalogService) {
        this.reservationService = reservationService;
        this.roomCatalogService = roomCatalogService;
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

        cancel.addThemeVariants(ButtonVariant.ERROR);
        anonymize.addThemeVariants(ButtonVariant.ERROR);
        var actions = new HorizontalLayout(change, cancel, anonymize);

        grid.addColumn(ReservationListItem::bookingNumber).setHeader("Booking number");
        grid.addColumn(ReservationListItem::guestName).setHeader("Guest");
        grid.addColumn(ReservationListItem::roomNumber).setHeader("Room");
        grid.addColumn(ReservationListItem::arrival).setHeader("Arrival");
        grid.addColumn(ReservationListItem::departure).setHeader("Departure");
        grid.addColumn(ReservationListItem::numberOfGuests).setHeader("Guests");
        grid.addColumn(ReservationListItem::totalPrice).setHeader("Total (CHF)");
        grid.addColumn(r -> r.status() + (r.anonymized() ? " (anonymized)" : "")).setHeader("Status");
        grid.asSingleSelect().addValueChangeListener(e -> updateActions(e.getValue()));

        add(filters, actions, noReservations, grid);
        refresh();
    }

    private void updateActions(ReservationListItem selected) {
        change.setEnabled(selected != null && selected.changeable());
        cancel.setEnabled(selected != null && selected.cancellable());
        anonymize.setEnabled(selected != null && selected.anonymizable());
    }

    private void openChange() {
        new ChangeReservationDialog(reservationService, roomCatalogService.findRooms(), selected(), this::refresh)
                .open();
    }

    private void confirmCancel() {
        var bookingNumber = selected().bookingNumber();
        confirm("Cancel reservation", "Cancel reservation %s?".formatted(bookingNumber),
                _ -> apply(() -> reservationService.cancelReservation(bookingNumber), "Reservation cancelled"));
    }

    private void confirmAnonymize() {
        var bookingNumber = selected().bookingNumber();
        confirm("Anonymize guest data",
                "Replace name, email and phone of reservation %s with \"Anonymized\"? This cannot be undone."
                        .formatted(bookingNumber),
                _ -> apply(() -> reservationService.anonymizeGuest(bookingNumber), "Guest data anonymized"));
    }

    private void confirm(String header, String text, Consumer<ConfirmDialog.ConfirmEvent> onYes) {
        var dialog = new ConfirmDialog(header, text, "Yes", onYes::accept);
        dialog.setCancelable(true);
        dialog.setCancelText("No");
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void apply(Runnable action, String successMessage) {
        try {
            action.run();
            Notification.show(successMessage);
        } catch (ReservationNotModifiableException e) {
            Notification.show(e.getMessage());
        }
        refresh();
    }

    private ReservationListItem selected() {
        return grid.asSingleSelect().getValue();
    }

    private void refresh() {
        var reservations = reservationService.findReservations(new ReservationFilter(bookingNumber.getValue(),
                guestName.getValue(), from.getValue(), to.getValue(), showCancelled.getValue()));
        grid.setItems(reservations);
        noReservations.setVisible(reservations.isEmpty());
        updateActions(null);
    }
}
