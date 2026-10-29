package ch.martinelli.edu.hrs.reservation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;

/**
 * Books one available room for a guest. Runs {@code onClose} after a booking or a conflict so the search refreshes.
 */
class BookingDialog extends Dialog {

    private final ReservationService reservationService;
    private final AvailableRoom room;
    private final StayPeriod period;
    private final Runnable onClose;

    private final IntegerField guests = new IntegerField("Guests");
    // validation only: the values are read from the fields
    private final Binder<Object> binder = new Binder<>();
    private final GuestFields guestFields = new GuestFields(binder);

    BookingDialog(ReservationService reservationService, AvailableRoom room, StayPeriod period, int numberOfGuests,
                  Runnable onClose) {
        this.reservationService = reservationService;
        this.room = room;
        this.period = period;
        this.onClose = onClose;

        setHeaderTitle("Book room %s, %s to %s".formatted(room.number(), period.arrival(), period.departure()));
        guests.setValue(numberOfGuests);
        binder.forField(guests)
                .asRequired("Guests is required")
                .withValidator(n -> n >= 1, "At least 1 guest")
                .bind(_ -> null, (_, _) -> {
                });
        var form = new FormLayout(guests);
        form.add(guestFields.components());
        add(form);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(new Button("Cancel", _ -> close()), save);
    }

    private void save() {
        if (!binder.validate().isOk()) {
            return;
        }
        try {
            var bookingNumber = reservationService.createReservation(room.roomId(), period, guests.getValue(),
                    guestFields.value());
            Notification.show("Reservation confirmed: " + bookingNumber);
            closeAndRefresh();
        } catch (CapacityExceededException e) {
            guests.setErrorMessage(e.getMessage());
            guests.setInvalid(true);
        } catch (RoomNotAvailableException e) {
            Notification.show(e.getMessage());
            closeAndRefresh();
        }
    }

    private void closeAndRefresh() {
        close();
        onClose.run();
    }
}
