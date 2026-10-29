package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.GuestFields;
import ch.martinelli.edu.hrs.reservation.StayPeriod;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.Binder;

/**
 * Collects the guest data, books the room type and then shows the booking number. Runs {@code onClose} when closed
 * so the search refreshes its free-room counts.
 */
class GuestBookingDialog extends Dialog {

    static final String PRIVACY_NOTE =
            "We only store your name, email and phone to manage your reservation.";
    static final String KEEP_BOOKING_NUMBER =
            "Please keep your booking number; you need it with your email to view or cancel your reservation.";

    private final GuestBookingService guestBookingService;
    private final RoomTypeAvailability roomType;
    private final StayPeriod period;
    private final int numberOfGuests;

    // validation only: the values are read from the fields
    private final Binder<Object> binder = new Binder<>();
    private final GuestFields guestFields = new GuestFields(binder);
    private final Button book = new Button("Book", _ -> book());

    GuestBookingDialog(GuestBookingService guestBookingService, RoomTypeAvailability roomType, StayPeriod period,
                       int numberOfGuests, Runnable onClose) {
        this.guestBookingService = guestBookingService;
        this.roomType = roomType;
        this.period = period;
        this.numberOfGuests = numberOfGuests;
        addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                onClose.run();
            }
        });

        setHeaderTitle("Book %s, %s to %s".formatted(roomType.roomTypeName(), period.arrival(), period.departure()));
        add(new Paragraph("%d guest(s), total CHF %s".formatted(numberOfGuests, roomType.totalPrice().toPlainString())),
                new FormLayout(guestFields.components()), new Paragraph(PRIVACY_NOTE));

        book.addThemeVariants(ButtonVariant.PRIMARY);
        // blocks a double click from creating two reservations
        book.setDisableOnClick(true);
        getFooter().add(new Button("Cancel", _ -> close()), book);
    }

    private void book() {
        if (!binder.validate().isOk()) {
            book.setEnabled(true);
            return;
        }
        try {
            var reservation = guestBookingService.bookRoomType(roomType.roomTypeName(), period, numberOfGuests,
                    guestFields.value());
            showConfirmation(reservation);
        } catch (RoomTypeNotAvailableException e) {
            Notification.show(e.getMessage());
            close();
        }
    }

    private void showConfirmation(GuestReservation reservation) {
        setHeaderTitle("Reservation confirmed");
        removeAll();
        add(new ReservationDetails(reservation), new Paragraph(KEEP_BOOKING_NUMBER));
        getFooter().removeAll();
        getFooter().add(new Button("Close", _ -> close()));
    }
}
