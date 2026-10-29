package ch.martinelli.edu.hrs.guestbooking;

import ch.martinelli.edu.hrs.reservation.ReservationNotModifiableException;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Public lookup and cancellation of a guest's own reservation with booking number and email.
 */
@PageTitle("My reservation")
@Route(value = "booking/reservation", autoLayout = false)
public class GuestReservationView extends VerticalLayout {

    static final String NOT_FOUND = "No reservation found for this booking number and email";

    private final GuestBookingService guestBookingService;

    private final TextField bookingNumber = new TextField("Booking number");
    // plain text field: a format error would hint at which value is wrong
    private final TextField email = new TextField("Email");
    private final Div result = new Div();
    // validation only: values are read from the fields when searching
    private final Binder<Object> binder = new Binder<>();

    // the values of the last lookup; the fields may have been edited since
    private String searchedBookingNumber;
    private String searchedEmail;

    public GuestReservationView(GuestBookingService guestBookingService) {
        this.guestBookingService = guestBookingService;

        binder.forField(bookingNumber).asRequired("Booking number is required").bind(_ -> null, (_, _) -> {
        });
        binder.forField(email).asRequired("Email is required").bind(_ -> null, (_, _) -> {
        });

        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.PRIMARY);
        var criteria = new HorizontalLayout(bookingNumber, email, search);
        criteria.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        criteria.setWrap(true);

        add(new H1("My reservation"), new RouterLink("Book a room", GuestSearchView.class), criteria, result);
    }

    private void search() {
        result.removeAll();
        if (!binder.validate().isOk()) {
            return;
        }
        searchedBookingNumber = bookingNumber.getValue();
        searchedEmail = email.getValue();
        show();
    }

    private void show() {
        result.removeAll();
        guestBookingService.findByBookingNumberAndEmail(searchedBookingNumber, searchedEmail).ifPresentOrElse(
                reservation -> {
                    result.add(new ReservationDetails(reservation));
                    if (guestBookingService.canCancel(reservation)) {
                        var cancel = new Button("Cancel reservation", _ -> confirmCancel(reservation));
                        cancel.addThemeVariants(ButtonVariant.ERROR);
                        result.add(cancel);
                    } else if (reservation.status() == ReservationStatus.CONFIRMED) {
                        result.add(new Span(CancellationDeadlinePassedException.MESSAGE));
                    }
                },
                () -> result.add(new Span(NOT_FOUND)));
    }

    private void confirmCancel(GuestReservation reservation) {
        var dialog = new ConfirmDialog("Cancel reservation",
                "Cancel reservation %s?".formatted(reservation.bookingNumber()), "Yes", _ -> cancel());
        dialog.setCancelable(true);
        dialog.setCancelText("No");
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void cancel() {
        try {
            guestBookingService.cancelByGuest(searchedBookingNumber, searchedEmail);
            Notification.show("Reservation cancelled");
        } catch (CancellationDeadlinePassedException | ReservationNotModifiableException e) {
            Notification.show(e.getMessage());
        }
        show();
    }
}
