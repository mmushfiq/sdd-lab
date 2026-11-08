package ch.martinelli.edu.hrs.guest;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.reservation.AvailabilityService;
import ch.martinelli.edu.hrs.reservation.AvailableRoom;
import ch.martinelli.edu.hrs.reservation.BookingRequest;
import ch.martinelli.edu.hrs.reservation.BookingConfirmationEmail;
import ch.martinelli.edu.hrs.reservation.BookingSource;
import ch.martinelli.edu.hrs.reservation.ReservationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * Public booking page: a Guest searches free rooms and books one without logging in (User Story 3).
 */
@Route("")
@PageTitle("Book a room | Hotel Reservation System")
@AnonymousAllowed
public class GuestBookingView extends VerticalLayout {

    private final transient AvailabilityService availabilityService;
    private final transient ReservationService reservationService;
    private final transient BookingConfirmationEmail confirmationEmail;
    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Grid<AvailableRoom> grid = new Grid<>();
    private final Paragraph noRoom = new Paragraph("No room is available for these dates and number of guests.");
    private final Paragraph confirmation = new Paragraph();
    // The stay the grid shows; booking uses it, not the fields, which may have changed since
    private Search shownSearch;

    private record Search(LocalDate arrival, LocalDate departure, int guests) {
    }

    public GuestBookingView(AvailabilityService availabilityService, ReservationService reservationService,
                            BookingConfirmationEmail confirmationEmail, Clock clock) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;
        this.confirmationEmail = confirmationEmail;

        var today = LocalDate.now(clock);
        arrival.setValue(today);
        departure.setValue(today.plusDays(1));
        guests.setValue(1);
        guests.setMin(1);
        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var searchBar = new HorizontalLayout(arrival, departure, guests, search);
        searchBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        searchBar.setWrap(true);

        grid.addColumn(AvailableRoom::roomNumber).setHeader("Room");
        grid.addColumn(AvailableRoom::roomTypeName).setHeader("Room type");
        grid.addColumn(AvailableRoom::capacity).setHeader("Capacity");
        grid.addColumn(AvailableRoom::totalPrice).setHeader("Total price");
        var book = new Button("Book", _ -> openBookingDialog(grid.asSingleSelect().getValue()));
        book.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> book.setEnabled(e.getValue() != null));
        noRoom.setVisible(false);
        confirmation.setVisible(false);

        // Where a guest views or cancels a booking later (FR-015)
        var myReservation = new RouterLink("View or cancel my reservation", MyReservationView.class);
        add(new H1("Book a room"), myReservation, confirmation, searchBar, noRoom, grid, book);
    }

    private void search() {
        show(new Search(arrival.getValue(), departure.getValue(), guests.getValue() == null ? 0 : guests.getValue()));
    }

    private void show(Search search) {
        try {
            var rooms = availabilityService.search(search.arrival(), search.departure(), search.guests());
            grid.setItems(rooms);
            noRoom.setVisible(rooms.isEmpty());
            shownSearch = search;
        } catch (BusinessRuleException e) {
            grid.setItems();
            noRoom.setVisible(false);
            shownSearch = null;
            showError(e.getMessage());
        }
    }

    private void openBookingDialog(AvailableRoom room) {
        var stay = shownSearch;
        var dialog = new Dialog("Book room " + room.roomNumber());
        var guestName = new TextField("Guest name");
        guestName.setMaxLength(100);
        var email = new EmailField("Email");
        email.setMaxLength(254);
        var phone = new TextField("Phone");
        phone.setMaxLength(30);

        // The jOOQ record only holds the form values; the service creates the reservation
        var binder = new Binder<ReservationRecord>();
        binder.forField(guestName).asRequired("Guest name is required")
                .bind(ReservationRecord::getGuestName, ReservationRecord::setGuestName);
        binder.forField(email).asRequired("Email is required")
                .withValidator(new EmailValidator("Enter a valid email"))
                .bind(ReservationRecord::getGuestEmail, ReservationRecord::setGuestEmail);
        binder.forField(phone).asRequired("Phone is required")
                .bind(ReservationRecord::getGuestPhone, ReservationRecord::setGuestPhone);

        var confirm = new Button("Confirm booking", _ -> {
            var guest = new ReservationRecord();
            if (!binder.writeBeanIfValid(guest)) {
                return;
            }
            try {
                var reservation = reservationService.book(new BookingRequest(room.roomId(), stay.arrival(),
                        stay.departure(), stay.guests(), guest.getGuestName(), guest.getGuestEmail(),
                        guest.getGuestPhone(), BookingSource.ONLINE));
                // book() has committed, so a mail failure cannot undo the booking (002 FR-006)
                confirmationEmail.send(reservation, room.roomTypeName());
                confirmation.setText("Your room is booked. Your reservation number is "
                        + reservation.getReservationNumber() + ". Use it with your email under "
                        + "\"View or cancel my reservation\". Total price: "
                        + reservation.getTotalPrice().toPlainString() + ".");
                confirmation.setVisible(true);
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
            // Success or not, the guest sees the current availability and can search again (US3/AC4)
            dialog.close();
            show(stay);
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new FormLayout(guestName, email, phone));
        dialog.getFooter().add(new Button("Cancel", _ -> dialog.close()), confirm);
        dialog.open();
    }
}
