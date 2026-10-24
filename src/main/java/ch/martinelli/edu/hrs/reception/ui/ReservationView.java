package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reservation.ReservationAction;
import ch.martinelli.edu.hrs.reservation.ReservationDetail;
import ch.martinelli.edu.hrs.reservation.ReservationSearchService;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

/**
 * UC-007 step 5: displays a reservation with all its data, the guest's contact data, the assigned room and the
 * actions its status allows. The actions are the entry points of UC-005, UC-006, UC-009, UC-010 and UC-012.
 */
@Route(value = "reservation-details", layout = StaffLayout.class)
@PageTitle("Reservation")
// NFR-009: Only the receptionist can open reservations.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class ReservationView extends VerticalLayout implements HasUrlParameter<String> {

    static final String NOT_FOUND = "No reservation has this reservation code.";

    private final ReservationSearchService searchService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM,
            FormatStyle.SHORT);
    private final Div content = new Div();

    public ReservationView(ReservationSearchService searchService) {
        this.searchService = searchService;
        add(new RouterLink("Back to reservation search", SearchReservationsView.class));
        content.setWidthFull();
        add(content);
    }

    @Override
    public void setParameter(BeforeEvent event, String reservationCode) {
        content.removeAll();
        searchService.findReservation(reservationCode).ifPresentOrElse(this::show, () -> {
            Paragraph notFound = new Paragraph(NOT_FOUND);
            notFound.setId("reservation-not-found");
            content.add(new H1("Reservation"), notFound);
        });
    }

    private void show(ReservationDetail reservation) {
        content.add(new H1("Reservation " + reservation.reservationCode()));
        content.add(
                field("status", "Status", statusText(reservation.status())),
                field("arrival-date", "Arrival date", dateFormatter.format(reservation.arrivalDate())),
                field("departure-date", "Departure date", dateFormatter.format(reservation.departureDate())),
                field("room-type", "Room type", reservation.roomTypeName()),
                field("number-of-guests", "Number of guests", String.valueOf(reservation.numberOfGuests())),
                field("total-price", "Total price", chf(reservation.totalPrice())),
                field("channel", "Channel", switch (reservation.channel()) {
                    case ONLINE -> "Online";
                    case RECEPTION -> "Reception";
                }),
                field("created-at", "Created", dateTime(reservation.createdAt())));
        // The assigned room when a room is assigned (Checked In and Checked Out)
        if (reservation.roomNumber() != null) {
            content.add(field("room", "Room", reservation.roomNumber()));
        }
        if (reservation.checkedInAt() != null) {
            content.add(field("checked-in-at", "Checked in", dateTime(reservation.checkedInAt())));
        }
        if (reservation.checkedOutAt() != null) {
            content.add(field("checked-out-at", "Checked out", dateTime(reservation.checkedOutAt())));
        }
        if (reservation.cancelledAt() != null) {
            content.add(field("cancelled-at", "Cancelled", dateTime(reservation.cancelledAt())));
        }

        content.add(new H2("Guest"),
                field("guest-name", "Name", reservation.guestFirstName() + " " + reservation.guestLastName()),
                field("guest-email", "Email address", reservation.guestEmail()),
                field("guest-phone", "Phone number", reservation.guestPhone()));

        // The actions the status allows
        List<ReservationAction> actions = ReservationAction.allowedFor(reservation.status());
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setId("actions");
        actions.forEach(action -> actionButtons.add(actionButton(action)));
        if (!actions.isEmpty()) {
            content.add(new H2("Actions"), actionButtons);
        }
    }

    /**
     * The button of an action. Actions whose use case is not implemented yet are shown disabled.
     */
    private static Button actionButton(ReservationAction action) {
        Button button = new Button(switch (action) {
            case CHANGE -> "Change";
            case CANCEL -> "Cancel reservation";
            case CHECK_IN -> "Check in";
            case CHECK_OUT -> "Check out";
            case RECORD_NO_SHOW -> "Record no-show";
        });
        button.setEnabled(false);
        return button;
    }

    static String statusText(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "Confirmed";
            case CHECKED_IN -> "Checked In";
            case CHECKED_OUT -> "Checked Out";
            case CANCELLED -> "Cancelled";
            case NO_SHOW -> "No-Show";
        };
    }

    private static Paragraph field(String id, String label, String value) {
        Paragraph paragraph = new Paragraph(label + ": " + value);
        paragraph.setId(id);
        return paragraph;
    }

    private String dateTime(LocalDateTime value) {
        return dateTimeFormatter.format(value);
    }

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }
}
