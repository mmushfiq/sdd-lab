package ch.martinelli.edu.hrs.reservation.ui;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.AvailabilityService.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.ReservationException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationService.Guest;
import ch.martinelli.edu.hrs.reservation.ReservationService.Stay;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static ch.martinelli.edu.hrs.room.ui.RoomTypeView.formatRate;

/**
 * Creates a reservation (availability search, then guest data), or shows an existing one. A Booked reservation can
 * be changed (the search excludes the reservation itself) or cancelled; other reservations are read-only.
 */
class ReservationDialog extends Dialog {

    static final String INVALID_EMAIL = "Enter a valid email address";
    static final String DATES_REQUIRED = "Arrival and departure date are required";

    private final ReservationService reservationService;
    private final AvailabilityService availabilityService;
    private final ReservationRecord reservation;
    private final Runnable onChange;

    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Span availabilityMessage = new Span();
    private final Grid<AvailableRoom> rooms = new Grid<>();
    private final TextField guestName = new TextField("Guest name");
    private final TextField guestEmail = new TextField("Email");
    private final TextField guestPhone = new TextField("Phone");
    private final Binder<ReservationRecord> guestBinder = new Binder<>();
    private Stay searched;

    ReservationDialog(ReservationService reservationService, AvailabilityService availabilityService,
                      Map<Long, String> roomNumbers, ReservationRecord reservation, Runnable onChange) {
        this.reservationService = reservationService;
        this.availabilityService = availabilityService;
        this.reservation = reservation;
        this.onChange = onChange;
        setWidth("min(60rem, 100vw)");

        Button close = new Button("Close", _ -> close());
        close.setId("close-reservation");
        getFooter().add(close);

        if (reservation != null) {
            setHeaderTitle("Reservation " + reservation.getReservationNumber());
            add(details(reservation, roomNumbers.get(reservation.getRoomId())));
            if (!ReservationService.BOOKED.equals(reservation.getStatus())) {
                return;
            }
        } else {
            setHeaderTitle("New reservation");
        }

        Button searchButton = new Button("Search availability", _ -> search());
        searchButton.setId("search-availability");
        guests.setMin(1);
        guests.setStepButtonsVisible(true);
        HorizontalLayout searchBar = new HorizontalLayout(arrival, departure, guests, searchButton);
        searchBar.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        availabilityMessage.setId("availability-message");

        rooms.setId("available-rooms");
        rooms.addColumn(AvailableRoom::roomNumber).setHeader("Room");
        rooms.addColumn(AvailableRoom::roomTypeName).setHeader("Room type");
        rooms.addColumn(AvailableRoom::capacity).setHeader("Capacity");
        rooms.addColumn(r -> formatRate(r.nightlyRate())).setHeader("Nightly rate");
        rooms.addColumn(r -> formatRate(r.totalPrice())).setHeader("Total price");
        rooms.setAllRowsVisible(true);
        rooms.setVisible(false);
        add(searchBar, availabilityMessage, rooms);

        if (reservation == null) {
            guestName.setMaxLength(100);
            guestEmail.setMaxLength(254);
            guestPhone.setMaxLength(30);
            guestBinder.forField(guestName).asRequired("Guest name is required")
                    .withValidator(v -> !v.isBlank(), "Guest name is required")
                    .bind(ReservationRecord::getGuestName, ReservationRecord::setGuestName);
            guestBinder.forField(guestEmail).asRequired("Email is required")
                    .withValidator(new EmailValidator(INVALID_EMAIL))
                    .bind(ReservationRecord::getGuestEmail, ReservationRecord::setGuestEmail);
            guestBinder.forField(guestPhone)
                    .bind(ReservationRecord::getGuestPhone, ReservationRecord::setGuestPhone);
            FormLayout guestForm = new FormLayout(guestName, guestEmail, guestPhone);
            Button create = new Button("Create reservation", _ -> create());
            create.setId("create-reservation");
            create.addThemeVariants(ButtonVariant.PRIMARY);
            Div guestStep = new Div(guestForm, create);
            guestStep.setVisible(false);
            rooms.asSingleSelect().addValueChangeListener(e -> guestStep.setVisible(e.getValue() != null));
            add(guestStep);
            arrival.setValue(availabilityService.today());
            departure.setValue(arrival.getValue().plusDays(1));
            guests.setValue(1);
        } else {
            arrival.setValue(reservation.getArrivalDate());
            departure.setValue(reservation.getDepartureDate());
            guests.setValue(reservation.getNumberOfGuests());
            Button save = new Button("Save changes", _ -> saveChanges());
            save.setId("save-reservation");
            save.addThemeVariants(ButtonVariant.PRIMARY);
            Button cancelReservation = new Button("Cancel reservation", _ -> confirmCancel());
            cancelReservation.setId("cancel-reservation");
            cancelReservation.addThemeVariants(ButtonVariant.ERROR);
            getFooter().addComponentAsFirst(new HorizontalLayout(save, cancelReservation));
            search();
            rooms.getListDataView().getItems()
                    .filter(r -> r.roomId() == reservation.getRoomId())
                    .findFirst().ifPresent(rooms::select);
        }
    }

    private static Div details(ReservationRecord r, String roomNumber) {
        Div details = new Div(
                new Paragraph("Reservation number: " + r.getReservationNumber()),
                new Paragraph("Status: " + ReservationView.statusLabel(r.getStatus())),
                new Paragraph("Room: " + roomNumber),
                new Paragraph("Arrival: " + r.getArrivalDate()),
                new Paragraph("Departure: " + r.getDepartureDate()),
                new Paragraph("Guests: " + r.getNumberOfGuests()),
                new Paragraph("Guest name: " + r.getGuestName()),
                new Paragraph("Email: " + r.getGuestEmail()),
                new Paragraph("Phone: " + (r.getGuestPhone() == null ? "" : r.getGuestPhone())),
                new Paragraph("Nightly rate: " + formatRate(r.getNightlyRate())),
                new Paragraph("Total price: " + formatRate(
                        AvailabilityService.totalPrice(r.getNightlyRate(), r.getArrivalDate(), r.getDepartureDate()))));
        details.setId("reservation-details");
        return details;
    }

    private void search() {
        searched = null;
        rooms.setVisible(false);
        rooms.setItems(List.of());
        if (arrival.isEmpty() || departure.isEmpty()) {
            availabilityMessage.setText(DATES_REQUIRED);
            return;
        }
        List<AvailableRoom> found;
        try {
            found = availabilityService.findAvailableRooms(arrival.getValue(), departure.getValue(),
                    guests.getValue(), reservation == null ? null : reservation.getId());
        } catch (ReservationException e) {
            availabilityMessage.setText(e.getMessage());
            return;
        }
        if (found.isEmpty()) {
            availabilityMessage.setText(AvailabilityService.NO_ROOMS);
            return;
        }
        availabilityMessage.setText("");
        searched = new Stay(0, arrival.getValue(), departure.getValue(), guests.getValue());
        rooms.setItems(found);
        rooms.setVisible(true);
    }

    /**
     * The stay as searched, in the room chosen from the result, or null with a message when no room is chosen.
     */
    private Stay chosenStay() {
        AvailableRoom room = rooms.asSingleSelect().getValue();
        if (searched == null || room == null) {
            showError("Search availability and choose a room");
            return null;
        }
        return new Stay(room.roomId(), searched.arrival(), searched.departure(), searched.guests());
    }

    private void create() {
        ReservationRecord form = new ReservationRecord();
        Stay stay = chosenStay();
        if (stay == null || !guestBinder.writeBeanIfValid(form)) {
            return;
        }
        try {
            ReservationRecord created = reservationService.create(stay,
                    new Guest(form.getGuestName(), form.getGuestEmail(), form.getGuestPhone()));
            done("Reservation " + created.getReservationNumber() + " created");
        } catch (ReservationException e) {
            showError(e.getMessage());
        }
    }

    private void saveChanges() {
        Stay stay = chosenStay();
        if (stay == null) {
            return;
        }
        try {
            reservationService.change(reservation.getId(), stay);
            done("Reservation " + reservation.getReservationNumber() + " changed");
        } catch (ReservationException e) {
            showError(e.getMessage());
        }
    }

    private void confirmCancel() {
        ConfirmDialog confirm = new ConfirmDialog("Cancel reservation",
                "Cancel reservation " + reservation.getReservationNumber() + "?",
                "Cancel reservation", _ -> cancelReservation());
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.setCancelText("Keep reservation");
        confirm.open();
    }

    private void cancelReservation() {
        try {
            reservationService.cancel(reservation.getId());
            done("Reservation " + reservation.getReservationNumber() + " cancelled");
        } catch (ReservationException e) {
            showError(e.getMessage());
        }
    }

    private void done(String message) {
        Notification.show(message).addThemeVariants(NotificationVariant.SUCCESS);
        close();
        onChange.run();
    }

    private static void showError(String message) {
        Notification.show(message).addThemeVariants(NotificationVariant.ERROR);
    }
}
