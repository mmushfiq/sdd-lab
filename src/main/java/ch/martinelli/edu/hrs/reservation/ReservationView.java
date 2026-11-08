package ch.martinelli.edu.hrs.reservation;

import ch.martinelli.edu.hrs.core.BusinessRuleException;
import ch.martinelli.edu.hrs.core.ui.MainLayout;
import ch.martinelli.edu.hrs.db.tables.records.ReservationRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.martinelli.edu.hrs.core.ui.Notifications.showError;

/**
 * The Receptionist searches free rooms and books one for a phone or walk-in guest (User Story 2), and finds,
 * changes and cancels reservations (User Story 6).
 */
@Route(value = "reception/reservations", layout = MainLayout.class)
@PageTitle("Reservations | Hotel Reservation System")
@Menu(title = "Reservations", order = 11)
@RolesAllowed("RECEPTIONIST")
public class ReservationView extends VerticalLayout {

    private final transient AvailabilityService availabilityService;
    private final transient ReservationService reservationService;
    private final transient RoomService roomService;
    private final DatePicker arrival = new DatePicker("Arrival");
    private final DatePicker departure = new DatePicker("Departure");
    private final IntegerField guests = new IntegerField("Guests");
    private final Grid<AvailableRoom> grid = new Grid<>();
    private final Paragraph noRoom = new Paragraph("No room is available for these dates and number of guests.");
    private final TextField findText = new TextField("Reservation number or guest name");
    private final Grid<FoundReservation> found = new Grid<>();
    // The stay the grid shows; booking uses it, not the fields, which may have changed since (US2/AC2)
    private Search shownSearch;

    private record Search(LocalDate arrival, LocalDate departure, int guests) {
    }

    public ReservationView(AvailabilityService availabilityService, ReservationService reservationService,
                           RoomService roomService, Clock clock) {
        this.availabilityService = availabilityService;
        this.reservationService = reservationService;
        this.roomService = roomService;

        var today = LocalDate.now(clock);
        arrival.setValue(today);
        departure.setValue(today.plusDays(1));
        guests.setValue(1);
        guests.setMin(1);
        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        var searchBar = new HorizontalLayout(arrival, departure, guests, search);
        searchBar.setAlignItems(FlexComponent.Alignment.BASELINE);

        grid.setId("available");
        grid.addColumn(AvailableRoom::roomNumber).setHeader("Room");
        grid.addColumn(AvailableRoom::roomTypeName).setHeader("Room type");
        grid.addColumn(AvailableRoom::capacity).setHeader("Capacity");
        grid.addColumn(AvailableRoom::nightlyRate).setHeader("Nightly rate");
        grid.addColumn(AvailableRoom::totalPrice).setHeader("Total price");
        var book = new Button("Book", _ -> openBookingDialog(grid.asSingleSelect().getValue()));
        book.setEnabled(false);
        grid.asSingleSelect().addValueChangeListener(e -> book.setEnabled(e.getValue() != null));
        noRoom.setVisible(false);

        add(new H2("New reservation"), searchBar, noRoom, grid, book,
                new H2("Find reservation"), findBar(), found, foundActions());
    }

    private HorizontalLayout findBar() {
        var find = new Button("Find", _ -> refreshFound());
        findText.setWidth("20em");
        var bar = new HorizontalLayout(findText, find);
        bar.setAlignItems(FlexComponent.Alignment.BASELINE);

        found.setId("found");
        found.setAllRowsVisible(true);
        found.addColumn(f -> f.reservation().getReservationNumber()).setHeader("Reservation");
        found.addColumn(f -> f.reservation().getGuestName()).setHeader("Guest");
        found.addColumn(FoundReservation::roomNumber).setHeader("Room");
        found.addColumn(f -> f.reservation().getArrivalDate()).setHeader("Arrival");
        found.addColumn(f -> f.reservation().getDepartureDate()).setHeader("Departure");
        found.addColumn(f -> f.reservation().getNumberOfGuests()).setHeader("Guests");
        found.addColumn(f -> f.reservation().getStatus()).setHeader("Status");
        found.addColumn(f -> f.reservation().getTotalPrice()).setHeader("Total price");
        return bar;
    }

    private HorizontalLayout foundActions() {
        var change = new Button("Change", _ -> openChangeDialog(found.asSingleSelect().getValue()));
        var cancel = new Button("Cancel reservation", _ -> confirmCancel(found.asSingleSelect().getValue()));
        cancel.addThemeVariants(ButtonVariant.LUMO_ERROR);
        change.setEnabled(false);
        cancel.setEnabled(false);
        // Only booked reservations can be changed or cancelled (FR-016); the service checks it again
        found.asSingleSelect().addValueChangeListener(e -> {
            var booked = e.getValue() != null
                    && ReservationStatus.BOOKED.name().equals(e.getValue().reservation().getStatus());
            change.setEnabled(booked);
            cancel.setEnabled(booked);
        });
        return new HorizontalLayout(change, cancel);
    }

    private void refreshFound() {
        found.setItems(reservationService.find(findText.getValue()));
    }

    private void openChangeDialog(FoundReservation selected) {
        var current = selected.reservation();
        var dialog = new Dialog("Change reservation " + current.getReservationNumber());
        var rooms = roomService.findAll().stream().collect(Collectors.toMap(RoomRecord::getId, Function.identity()));
        var room = new ComboBox<RoomRecord>("Room", rooms.values().stream()
                .sorted((a, b) -> a.getRoomNumber().compareTo(b.getRoomNumber())).toList());
        room.setItemLabelGenerator(RoomRecord::getRoomNumber);
        room.setId("change-room");
        var arrivalDate = new DatePicker("Arrival");
        arrivalDate.setId("change-arrival");
        var departureDate = new DatePicker("Departure");
        departureDate.setId("change-departure");
        var numberOfGuests = new IntegerField("Guests");
        numberOfGuests.setId("change-guests");
        numberOfGuests.setMin(1);
        var guestName = new TextField("Guest name");
        guestName.setMaxLength(100);
        var email = new EmailField("Email");
        email.setMaxLength(254);
        var phone = new TextField("Phone");
        phone.setMaxLength(30);

        var binder = new Binder<ReservationRecord>();
        binder.forField(room).asRequired("Room is required")
                .bind(r -> rooms.get(r.getRoomId()), (r, v) -> r.setRoomId(v == null ? null : v.getId()));
        binder.forField(arrivalDate).asRequired("Arrival is required")
                .bind(ReservationRecord::getArrivalDate, ReservationRecord::setArrivalDate);
        binder.forField(departureDate).asRequired("Departure is required")
                .bind(ReservationRecord::getDepartureDate, ReservationRecord::setDepartureDate);
        binder.forField(numberOfGuests).asRequired("Guests is required")
                .bind(ReservationRecord::getNumberOfGuests, ReservationRecord::setNumberOfGuests);
        binder.forField(guestName).asRequired("Guest name is required")
                .bind(ReservationRecord::getGuestName, ReservationRecord::setGuestName);
        binder.forField(email).asRequired("Email is required")
                .withValidator(new EmailValidator("Enter a valid email"))
                .bind(ReservationRecord::getGuestEmail, ReservationRecord::setGuestEmail);
        binder.forField(phone).asRequired("Phone is required")
                .bind(ReservationRecord::getGuestPhone, ReservationRecord::setGuestPhone);
        binder.readBean(current);

        var save = new Button("Save changes", _ -> {
            var edited = new ReservationRecord();
            if (!binder.writeBeanIfValid(edited)) {
                return;
            }
            try {
                reservationService.change(current.getId(), new BookingRequest(edited.getRoomId(),
                        edited.getArrivalDate(), edited.getDepartureDate(), edited.getNumberOfGuests(),
                        edited.getGuestName(), edited.getGuestEmail(), edited.getGuestPhone(), null));
                dialog.close();
                showSuccess("Reservation " + current.getReservationNumber() + " changed.");
                refreshFound();
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(new FormLayout(room, arrivalDate, departureDate, numberOfGuests, guestName, email, phone));
        dialog.getFooter().add(new Button("Close", _ -> dialog.close()), save);
        dialog.open();
    }

    private void confirmCancel(FoundReservation selected) {
        var number = selected.reservation().getReservationNumber();
        var confirm = new ConfirmDialog("Cancel reservation " + number + "?",
                "Its nights become available again.", "Cancel reservation", _ -> {
            try {
                reservationService.cancel(selected.reservation().getId());
                showSuccess("Reservation " + number + " cancelled.");
            } catch (BusinessRuleException e) {
                showError(e.getMessage());
            }
            refreshFound();
        });
        confirm.setConfirmButtonTheme("error primary");
        confirm.setCancelable(true);
        confirm.setCancelText("Keep reservation");
        confirm.open();
    }

    private static void showSuccess(String message) {
        Notification.show(message, 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
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
                        guest.getGuestPhone(), BookingSource.RECEPTION));
                dialog.close();
                Notification.show("Reservation " + reservation.getReservationNumber() + " booked.", 10000,
                        Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                show(stay);
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
