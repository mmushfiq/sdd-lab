package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.roomcatalog.RoomListItem;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;

import java.time.LocalDate;
import java.util.List;

/**
 * Changes dates, room, number of guests and guest data of a changeable reservation.
 * Runs {@code onChanged} after a successful change or when the reservation can no longer be changed.
 */
class ChangeReservationDialog extends Dialog {

    private final ReservationService reservationService;
    private final ReservationListItem reservation;
    private final Runnable onChanged;

    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final ComboBox<RoomListItem> room = new ComboBox<>("Room");
    private final IntegerField guests = new IntegerField("Guests");
    // validation only: the values are read from the fields
    private final Binder<Object> binder = new Binder<>();
    private final GuestFields guestFields = new GuestFields(binder);

    ChangeReservationDialog(ReservationService reservationService, List<RoomListItem> rooms,
                            ReservationListItem reservation, Runnable onChanged) {
        this.reservationService = reservationService;
        this.reservation = reservation;
        this.onChanged = onChanged;

        setHeaderTitle("Change reservation " + reservation.bookingNumber());
        room.setItems(rooms);
        room.setItemLabelGenerator(r -> "%s (%s)".formatted(r.number(), r.roomTypeName()));
        bindFields();

        arrival.setValue(reservation.arrival());
        // a staying guest keeps the arrival; only the departure may move
        arrival.setReadOnly(reservation.arrival().isBefore(LocalDate.now()));
        departure.setValue(reservation.departure());
        room.setValue(rooms.stream().filter(r -> r.id() == reservation.roomId()).findFirst().orElse(null));
        guests.setValue(reservation.numberOfGuests());
        guestFields.setValue(reservation.guest());

        var form = new FormLayout(arrival, departure, room, guests);
        form.add(guestFields.components());
        add(form);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(new Button("Cancel", _ -> close()), save);
    }

    private void bindFields() {
        binder.forField(arrival)
                .asRequired("Arrival is required")
                .withValidator(d -> d.equals(reservation.arrival()) || !d.isBefore(LocalDate.now()),
                        "Arrival must be today or later")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(departure)
                .asRequired("Departure is required")
                .withValidator(d -> arrival.getValue() == null || d.isAfter(arrival.getValue()),
                        "Departure must be after arrival")
                .withValidator(d -> !d.isBefore(LocalDate.now()), "Departure must be today or later")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(room)
                .asRequired("Room is required")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(guests)
                .asRequired("Guests is required")
                .withValidator(n -> n >= 1, "At least 1 guest")
                .bind(_ -> null, (_, _) -> {
                });
    }

    private void save() {
        if (!binder.validate().isOk()) {
            return;
        }
        try {
            reservationService.changeReservation(reservation.bookingNumber(), room.getValue().id(),
                    new StayPeriod(arrival.getValue(), departure.getValue()), guests.getValue(), guestFields.value());
            Notification.show("Reservation changed");
            close();
            onChanged.run();
        } catch (RoomNotAvailableException e) {
            Notification.show(e.getMessage());
        } catch (CapacityExceededException e) {
            guests.setErrorMessage(e.getMessage());
            guests.setInvalid(true);
        } catch (ReservationNotModifiableException e) {
            Notification.show(e.getMessage());
            close();
            onChanged.run();
        }
    }
}
