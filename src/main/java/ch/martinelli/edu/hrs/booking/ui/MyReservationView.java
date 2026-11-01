package ch.martinelli.edu.hrs.booking.ui;

import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.ReservationException;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import ch.martinelli.edu.hrs.reservation.ReservationService.GuestReservation;
import ch.martinelli.edu.hrs.reservation.ui.ReservationView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import static ch.martinelli.edu.hrs.room.ui.RoomTypeView.formatRate;

/**
 * Public page where a guest finds their reservation by reservation number plus booking email and may cancel it
 * before the arrival date.
 */
@Route(value = MyReservationView.ROUTE, layout = GuestLayout.class)
@PageTitle("My reservation")
@AnonymousAllowed
public class MyReservationView extends VerticalLayout {

    public static final String ROUTE = "my-reservation";

    private final ReservationService reservationService;

    private final TextField number = new TextField("Reservation number");
    private final TextField email = new TextField("Email");
    private final Span message = new Span();
    private final Div details = new Div();

    public MyReservationView(ReservationService reservationService) {
        this.reservationService = reservationService;

        number.setMaxLength(20);
        email.setMaxLength(254);
        Button find = new Button("Find", _ -> find());
        find.setId("find-reservation");
        find.addThemeVariants(ButtonVariant.PRIMARY);
        message.setId("my-reservation-message");
        details.setId("my-reservation-details");

        add(new H2("My reservation"), new FormLayout(number, email), find, message, details);
    }

    private void find() {
        find(number.getValue(), email.getValue());
    }

    private void find(String reservationNumber, String bookingEmail) {
        details.removeAll();
        message.setText("");
        reservationService.findForGuest(reservationNumber, bookingEmail)
                .ifPresentOrElse(found -> show(found, bookingEmail),
                        () -> message.setText(ReservationService.NOT_FOUND));
    }

    // The email that found the reservation; cancel uses it, not the field, which the guest may have edited since
    private void show(GuestReservation found, String bookingEmail) {
        ReservationRecord r = found.reservation();
        details.add(
                new Paragraph("Reservation number: " + r.getReservationNumber()),
                new Paragraph("Room type: " + found.roomTypeName()),
                new Paragraph("Arrival: " + r.getArrivalDate()),
                new Paragraph("Departure: " + r.getDepartureDate()),
                new Paragraph("Guests: " + r.getNumberOfGuests()),
                new Paragraph("Guest name: " + r.getGuestName()),
                new Paragraph("Total price: " + formatRate(
                        AvailabilityService.totalPrice(r.getNightlyRate(), r.getArrivalDate(), r.getDepartureDate()))),
                new Paragraph("Status: " + ReservationView.statusLabel(r.getStatus())));
        if (reservationService.guestMayCancel(r)) {
            Button cancel = new Button("Cancel reservation", _ -> confirmCancel(r, bookingEmail));
            cancel.setId("guest-cancel-reservation");
            cancel.addThemeVariants(ButtonVariant.ERROR);
            details.add(cancel);
        } else if (ReservationService.BOOKED.equals(r.getStatus())) {
            message.setText(ReservationService.CONTACT_HOTEL);
        }
    }

    private void confirmCancel(ReservationRecord r, String bookingEmail) {
        ConfirmDialog confirm = new ConfirmDialog("Cancel reservation",
                "Cancel reservation " + r.getReservationNumber() + "?",
                "Cancel reservation", _ -> cancel(r.getReservationNumber(), bookingEmail));
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.setCancelText("Keep reservation");
        confirm.open();
    }

    private void cancel(String reservationNumber, String bookingEmail) {
        try {
            reservationService.cancelByGuest(reservationNumber, bookingEmail);
        } catch (ReservationException e) {
            find(reservationNumber, bookingEmail);
            message.setText(e.getMessage());
            return;
        }
        find(reservationNumber, bookingEmail);
    }
}
