package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.Clock;
import java.time.LocalDate;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * The Receptionist searches free rooms and books one for a phone or walk-in guest (User Story 2).
 */
@Route(value = "reception/reservations", layout = MainLayout.class)
@PageTitle("Reservations | Hotel Reservation System")
@Menu(title = "Reservations", order = 11)
@RolesAllowed("RECEPTIONIST")
public class ReservationView extends VerticalLayout {

    private final transient AvailabilityService availabilityService;
    private final transient ReservationService reservationService;
    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Grid<AvailableRoom> grid = new Grid<>();
    private final Paragraph noRoom = new Paragraph("No room is available for these dates and number of guests.");

    public ReservationView(AvailabilityService availabilityService, ReservationService reservationService,
                           Clock clock) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;

        var today = LocalDate.now(clock);
        arrival.setValue(today);
        departure.setValue(today.plusDays(1));
        guests.setValue(1);
        guests.setMin(1);
        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var searchBar = new HorizontalLayout(arrival, departure, guests, search);
        searchBar.setAlignItems(FlexComponent.Alignment.BASELINE);

        grid.addColumn(AvailableRoom::roomNumber).setHeader("Room");
        grid.addColumn(AvailableRoom::roomTypeName).setHeader("Room type");
        grid.addColumn(AvailableRoom::capacity).setHeader("Capacity");
        grid.addColumn(AvailableRoom::nightlyRate).setHeader("Nightly rate");
        grid.addColumn(AvailableRoom::totalPrice).setHeader("Total price");
        var book = new Button("Book", _ -> openBookingDialog(grid.asSingleSelect().getValue()));
        book.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> book.setEnabled(e.getValue() != null));
        noRoom.setVisible(false);

        add(searchBar, noRoom, grid, book);
    }

    private void search() {
        try {
            var rooms = availabilityService.search(arrival.getValue(), departure.getValue(),
                    guests.getValue() == null ? 0 : guests.getValue());
            grid.setItems(rooms);
            noRoom.setVisible(rooms.isEmpty());
        } catch (BusinessRuleException e) {
            grid.setItems();
            noRoom.setVisible(false);
            showError(e.getMessage());
        }
    }

    private void openBookingDialog(AvailableRoom room) {
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
                var reservation = reservationService.book(new BookingRequest(room.roomId(), arrival.getValue(),
                        departure.getValue(), guests.getValue(), guest.getGuestName(), guest.getGuestEmail(),
                        guest.getGuestPhone(), BookingSource.RECEPTION));
                dialog.close();
                Notification.show("Reservation " + reservation.getReservationNumber() + " booked.", 10000,
                        Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                search();
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new FormLayout(guestName, email, phone));
        dialog.getFooter().add(new Button("Cancel", _ -> dialog.close()), confirm);
        dialog.open();
    }
}
