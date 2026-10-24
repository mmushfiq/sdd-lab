package ch.martinelli.edu.hrs.management.ui;

import ch.martinelli.edu.hrs.management.ErasableGuest;
import ch.martinelli.edu.hrs.management.GuestErasureService;
import ch.martinelli.edu.hrs.management.GuestHasOpenReservationsException;
import ch.martinelli.edu.hrs.management.GuestReservation;
import ch.martinelli.edu.hrs.management.GuestWithReservations;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC-024 Erase Guest Data: the manager erases a guest's personal data on the guest's request.
 */
@Route(value = "guest-erasure", layout = StaffLayout.class)
@PageTitle("Guest Erasure")
// NFR-009: Only the manager can erase guest data.
@RolesAllowed(StaffRole.MANAGER_ROLE)
public class GuestErasureView extends VerticalLayout {

    static final String NOT_FOUND = "No guest with this email address exists.";
    static final String OPEN_RESERVATIONS = "The guest has open reservations: %s. Cancel them or check the guest out "
            + "before the erasure.";
    static final String CANNOT_BE_UNDONE = "The guest's first name, last name, email address and phone number are "
            + "replaced. The erasure cannot be undone.";
    static final String ERASED = "The guest data is erased. The guest can no longer be identified.";

    private final GuestErasureService service;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final EmailField email = new EmailField("Email address");
    private final Div message = new Div();
    private final VerticalLayout details = new VerticalLayout();
    private final Paragraph name = field("guest-name");
    private final Paragraph guestEmail = field("guest-email");
    private final Paragraph phone = field("guest-phone");
    private final Grid<GuestReservation> reservations = new Grid<>();
    private final Button eraseButton = new Button("Erase guest data");

    private ErasableGuest guest;

    public GuestErasureView(GuestErasureService service) {
        this.service = service;
        add(new H1("Guest Erasure"));

        // Step 2
        email.setRequiredIndicatorVisible(true);
        email.setWidth("20rem");
        Button findButton = new Button("Find guest", _ -> find());
        findButton.addThemeVariants(ButtonVariant.PRIMARY);
        findButton.addClickShortcut(Key.ENTER);
        HorizontalLayout search = new HorizontalLayout(email, findButton);
        search.setAlignItems(FlexComponent.Alignment.BASELINE);
        search.setWrap(true);
        add(search);

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
        add(message);

        createDetails();
        add(details);
    }

    /**
     * Step 3: first name, last name, email address, phone number and all reservations.
     */
    private void createDetails() {
        reservations.setId("guest-reservations");
        reservations.addColumn(GuestReservation::reservationCode).setHeader("Reservation code").setAutoWidth(true);
        reservations.addColumn(reservation -> dateFormatter.format(reservation.arrivalDate())).setHeader("Arrival")
                .setAutoWidth(true);
        reservations.addColumn(reservation -> dateFormatter.format(reservation.departureDate()))
                .setHeader("Departure").setAutoWidth(true);
        reservations.addColumn(reservation -> statusText(reservation.status())).setHeader("Status")
                .setAutoWidth(true);
        reservations.setAllRowsVisible(true);
        reservations.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // Step 4
        eraseButton.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
        eraseButton.addClickListener(_ -> requestErasure());

        details.setPadding(false);
        details.add(new H2("Guest"), name, guestEmail, phone, new H2("Reservations"), reservations, eraseButton);
        details.setVisible(false);
    }

    /**
     * Steps 2 and 3, A1.
     */
    private void find() {
        message.removeAll();
        guest = null;
        details.setVisible(false);
        if (email.getValue().isBlank()) {
            email.setErrorMessage("Enter the email address");
            email.setInvalid(true);
            return;
        }
        email.setInvalid(false);
        service.findGuest(email.getValue()).ifPresentOrElse(this::showGuest,
                // A1: continue at step 2
                () -> showMessage(NOT_FOUND, true));
    }

    private void showGuest(GuestWithReservations found) {
        guest = found.guest();
        name.setText("Name: " + guest.firstName() + " " + guest.lastName());
        guestEmail.setText("Email address: " + guest.email());
        phone.setText("Phone number: " + guest.phone());
        reservations.setItems(found.reservations());
        eraseButton.setEnabled(true);
        details.setVisible(true);
    }

    /**
     * Steps 4 to 6, A2.
     */
    private void requestErasure() {
        message.removeAll();
        try {
            // Step 5
            service.checkErasable(guest.id());
        } catch (GuestHasOpenReservationsException e) {
            showOpenReservations(e.openReservations());
            return;
        }
        // Step 6
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Erase the data of " + guest.firstName() + " " + guest.lastName() + "?");
        dialog.setText(CANNOT_BE_UNDONE);
        dialog.setConfirmText("Erase guest data");
        dialog.setConfirmButtonTheme("error primary");
        // A3: the manager does not confirm; the guest data stays unchanged
        dialog.setCancelable(true);
        dialog.setCancelText("Keep guest data");
        dialog.addConfirmListener(_ -> confirmErasure());
        dialog.open();
    }

    /**
     * Steps 7 to 9.
     */
    private void confirmErasure() {
        try {
            service.erase(guest.id());
        } catch (GuestHasOpenReservationsException e) {
            // A2: a reservation was added or checked in since the check
            showOpenReservations(e.openReservations());
            return;
        }
        // Step 9
        guest = null;
        details.setVisible(false);
        email.clear();
        showMessage(ERASED, false);
    }

    /**
     * A2: the open reservations; the use case ends.
     */
    private void showOpenReservations(List<GuestReservation> open) {
        String codes = open.stream()
                .map(reservation -> reservation.reservationCode() + " (" + statusText(reservation.status()) + ")")
                .collect(Collectors.joining(", "));
        eraseButton.setEnabled(false);
        showMessage(OPEN_RESERVATIONS.formatted(codes), true);
    }

    private void showMessage(String text, boolean error) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("guest-erasure-message");
        if (error) {
            paragraph.getElement().setAttribute("role", "alert");
            paragraph.addClassNames(LumoUtility.TextColor.ERROR);
        }
        message.removeAll();
        message.add(paragraph);
    }

    private static Paragraph field(String id) {
        Paragraph paragraph = new Paragraph();
        paragraph.setId(id);
        return paragraph;
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
}
