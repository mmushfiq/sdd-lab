package ch.martinelli.edu.hrs.guestbooking;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;

/**
 * Read-only reservation details, shown after booking and on lookup.
 */
class ReservationDetails extends FormLayout {

    ReservationDetails(GuestReservation reservation) {
        addFormItem(new Span(reservation.bookingNumber()), "Booking number");
        addFormItem(new Span(reservation.arrival().toString()), "Arrival");
        addFormItem(new Span(reservation.departure().toString()), "Departure");
        addFormItem(new Span(reservation.roomTypeName()), "Room type");
        addFormItem(new Span(String.valueOf(reservation.numberOfGuests())), "Guests");
        addFormItem(new Span(reservation.totalPrice().toPlainString()), "Total (CHF)");
        addFormItem(new Span(reservation.status().name()), "Status");
    }
}
