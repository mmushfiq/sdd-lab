package ch.martinelli.edu.hrs.booking.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Layout of the public guest pages; no staff navigation. Vaadin also checks the layout's access annotation.
 */
@AnonymousAllowed
public class GuestLayout extends VerticalLayout implements RouterLayout {

    public GuestLayout() {
        H1 hotel = new H1("Hotel Reservation System");
        hotel.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");
        RouterLink book = new RouterLink("Book a room", BookingView.class);
        book.setId("nav-book");
        RouterLink myReservation = new RouterLink("My reservation", MyReservationView.class);
        myReservation.setId("nav-my-reservation");
        HorizontalLayout links = new HorizontalLayout(book, myReservation);
        add(hotel, links);
        setMaxWidth("60rem");
        getStyle().set("margin", "0 auto");
    }
}
