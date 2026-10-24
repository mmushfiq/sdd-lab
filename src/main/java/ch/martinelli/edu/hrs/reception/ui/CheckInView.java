package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reception.CheckInNotPossibleException;
import ch.martinelli.edu.hrs.reception.CheckInService;
import ch.martinelli.edu.hrs.reception.RoomCandidate;
import ch.martinelli.edu.hrs.reception.RoomNoLongerAssignableException;
import ch.martinelli.edu.hrs.reservation.ChangeableReservation;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-009 Check In Guest: the receptionist assigns a clean, free room of the reserved room type to an arriving guest.
 * <p>
 * Nothing is stored before the receptionist confirms a room, so leaving the view keeps the reservation unchanged (A5).
 */
@Route(value = "check-in", layout = StaffLayout.class)
@PageTitle("Check In")
// NFR-009: Only the receptionist can check in guests.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class CheckInView extends VerticalLayout implements HasUrlParameter<String> {

    static final String NOT_CONFIRMED = "A reservation with status %s cannot be checked in.";
    static final String NOT_ARRIVAL_DATE = "Check-in is only possible on the arrival date. "
            + "The dates can be changed with Change Reservation.";
    static final String NO_ROOM_READY = "No clean room of the room type %s is free.";
    static final String DIRTY_ROOMS = "Free rooms that are still dirty: %s.";
    static final String NO_DIRTY_ROOMS = "No free room of the room type is dirty either.";
    static final String ROOM_NO_LONGER_ASSIGNABLE = "Room %s can no longer be assigned. Please select another room.";

    private final CheckInService checkInService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Div message = new Div();
    private final Div rooms = new Div();

    private ChangeableReservation reservation;

    public CheckInView(CheckInService checkInService) {
        this.checkInService = checkInService;
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        rooms.setWidthFull();
    }

    @Override
    public void setParameter(BeforeEvent event, String reservationCode) {
        removeAll();
        message.removeAll();
        rooms.removeAll();
        checkInService.findReservation(reservationCode).ifPresentOrElse(this::show, () -> {
            Paragraph notFound = new Paragraph(ReservationView.NOT_FOUND);
            notFound.setId("reservation-not-found");
            add(new H1("Check In"), notFound);
        });
    }

    /**
     * Steps 2 to 4, A1, A2.
     */
    private void show(ChangeableReservation reservation) {
        this.reservation = reservation;
        add(new H1("Check In " + reservation.reservationCode()));
        add(field("room-type", "Room type", reservation.roomTypeName()),
                field("arrival-date", "Arrival date", dateFormatter.format(reservation.arrivalDate())),
                field("departure-date", "Departure date", dateFormatter.format(reservation.departureDate())),
                field("number-of-guests", "Number of guests", String.valueOf(reservation.numberOfGuests())));
        add(message, rooms);

        Button backButton = new Button("Back to reservation", _ -> showReservation());
        backButton.addThemeVariants(ButtonVariant.TERTIARY);

        try {
            checkInService.checkCheckInPossible(reservation);
        } catch (CheckInNotPossibleException e) {
            showError(switch (e.reason()) {
                // A1
                case NOT_CONFIRMED -> NOT_CONFIRMED.formatted(ReservationView.statusText(e.status()));
                // A2
                case NOT_ARRIVAL_DATE -> NOT_ARRIVAL_DATE;
            });
            add(backButton);
            return;
        }
        showRooms();
    }

    /**
     * Step 4 and A3: the assignable rooms with the first one by room number proposed.
     */
    private void showRooms() {
        rooms.removeAll();
        List<RoomCandidate> freeRooms = checkInService.freeRooms(reservation);
        List<RoomCandidate> cleanRooms = freeRooms.stream().filter(RoomCandidate::clean).toList();
        if (cleanRooms.isEmpty()) {
            // A3: no room ready; list the free rooms that are still dirty
            List<String> dirtyRooms = freeRooms.stream().map(RoomCandidate::roomNumber).toList();
            showError(NO_ROOM_READY.formatted(reservation.roomTypeName()));
            Paragraph dirty = new Paragraph(dirtyRooms.isEmpty() ? NO_DIRTY_ROOMS
                    : DIRTY_ROOMS.formatted(String.join(", ", dirtyRooms)));
            dirty.setId("dirty-rooms");
            Button backButton = new Button("Back to reservation", _ -> showReservation());
            rooms.add(dirty, backButton);
            return;
        }

        RadioButtonGroup<RoomCandidate> roomChoice = new RadioButtonGroup<>("Room");
        roomChoice.setItems(cleanRooms);
        roomChoice.setItemLabelGenerator(RoomCandidate::roomNumber);
        roomChoice.setValue(cleanRooms.getFirst());
        roomChoice.setRequiredIndicatorVisible(true);

        Button checkInButton = new Button("Check in", _ -> checkIn(roomChoice.getValue()));
        checkInButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A5: the receptionist stops the check-in; the reservation stays unchanged
        Button cancelButton = new Button("Cancel", _ -> showReservation());
        cancelButton.addThemeVariants(ButtonVariant.TERTIARY);
        rooms.add(roomChoice, new HorizontalLayout(checkInButton, cancelButton));
    }

    /**
     * Steps 5 to 8, A4.
     */
    private void checkIn(RoomCandidate room) {
        if (room == null) {
            return;
        }
        message.removeAll();
        try {
            checkInService.checkIn(reservation.reservationCode(), room.roomId());
        } catch (RoomNoLongerAssignableException _) {
            // A4: continue at step 4
            showError(ROOM_NO_LONGER_ASSIGNABLE.formatted(room.roomNumber()));
            showRooms();
            return;
        } catch (CheckInNotPossibleException e) {
            showError(e.reason() == CheckInNotPossibleException.Reason.NOT_CONFIRMED
                    ? NOT_CONFIRMED.formatted(ReservationView.statusText(e.status())) : NOT_ARRIVAL_DATE);
            rooms.removeAll();
            return;
        }

        // Step 8: the room number
        removeAll();
        add(new H1("Guest checked in"));
        Paragraph roomNumber = new Paragraph("Room: " + room.roomNumber());
        roomNumber.setId("checked-in-room");
        roomNumber.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
        add(roomNumber, new Paragraph("Reservation " + reservation.reservationCode()));
        add(new HorizontalLayout(
                new Button("Show reservation", _ -> showReservation()),
                new Button("Back to arrivals and departures",
                        _ -> UI.getCurrent().navigate(ArrivalsAndDeparturesView.class))));
    }

    private void showError(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("error-message");
        paragraph.getElement().setAttribute("role", "alert");
        paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        message.removeAll();
        message.add(paragraph);
    }

    private void showReservation() {
        UI.getCurrent().navigate(ReservationView.class, reservation.reservationCode());
    }

    private static Paragraph field(String id, String label, String value) {
        Paragraph paragraph = new Paragraph(label + ": " + value);
        paragraph.setId(id);
        return paragraph;
    }
}
