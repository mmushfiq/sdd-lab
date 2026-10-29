package ch.martinelli.edu.hrs.reservation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
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
    private final TextField name = new TextField("Name");
    private final EmailField email = new EmailField("Email");
    private final TextField phone = new TextField("Phone");
    // validation only: the Guest is built from the field values
    private final Binder<Object> binder = new Binder<>();

    BookingDialog(ReservationService reservationService, AvailableRoom room, StayPeriod period, int numberOfGuests,
                  Runnable onClose) {
        this.reservationService = reservationService;
        this.room = room;
        this.period = period;
        this.onClose = onClose;

        setHeaderTitle("Book room %s, %s to %s".formatted(room.number(), period.arrival(), period.departure()));
        guests.setValue(numberOfGuests);
        bindFields();
        add(new FormLayout(guests, name, email, phone));

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        getFooter().add(new Button("Cancel", _ -> close()), save);
    }

    private void bindFields() {
        binder.forField(guests)
                .asRequired("Guests is required")
                .withValidator(n -> n >= 1, "At least 1 guest")
                .bind(_ -> null, (_, _) -> {
                });
        bindText(name, "Name", 100);
        // the field's built-in format check runs first in Binder, so it carries the message
        email.setI18n(new EmailField.EmailFieldI18n().setPatternErrorMessage("Enter a valid email address"));
        binder.forField(email)
                .asRequired("Email is required")
                .withValidator(v -> v.length() <= 254, "Email can have at most 254 characters")
                .bind(_ -> null, (_, _) -> {
                });
        bindText(phone, "Phone", 30);
    }

    private void bindText(TextField field, String label, int maxLength) {
        binder.forField(field)
                .withValidator(v -> !v.isBlank(), label + " is required")
                .withValidator(v -> v.trim().length() <= maxLength,
                        "%s can have at most %d characters".formatted(label, maxLength))
                .bind(_ -> null, (_, _) -> {
                });
        field.setRequiredIndicatorVisible(true);
    }

    private void save() {
        if (!binder.validate().isOk()) {
            return;
        }
        var guest = new Guest(name.getValue().trim(), email.getValue().trim(), phone.getValue().trim());
        try {
            var bookingNumber = reservationService.createReservation(room.roomId(), period, guests.getValue(), guest);
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
