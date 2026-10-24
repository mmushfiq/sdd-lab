package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reception.CheckOutNotPossibleException;
import ch.martinelli.edu.hrs.reception.CheckOutPreview;
import ch.martinelli.edu.hrs.reception.CheckOutService;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * UC-010 Check Out Guest: the receptionist checks out a guest and sees the total price of the stay; the room is
 * released and marked dirty for housekeeping.
 * <p>
 * Nothing is stored before the receptionist confirms, so leaving the view keeps the reservation unchanged (A4).
 */
@Route(value = "check-out", layout = StaffLayout.class)
@PageTitle("Check Out")
// NFR-009: Only the receptionist can check out guests.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class CheckOutView extends VerticalLayout implements HasUrlParameter<String> {

    static final String NOT_CHECKED_IN = "Only a checked-in guest can be checked out. The reservation has status %s.";
    static final String EARLY = "The guest leaves early. The departure date and the total price are calculated for "
            + "the nights stayed.";
    static final String OVERDUE = "The guest stayed beyond the departure date. The departure date becomes today and "
            + "the total price includes the additional nights.";
    static final String NO_PAYMENT_RECORDED = "Collect the total price from the guest; the system records no payment.";

    private final CheckOutService checkOutService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private final Div message = new Div();

    private ChangeableReservation reservation;

    public CheckOutView(CheckOutService checkOutService) {
        this.checkOutService = checkOutService;
        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");
    }

    @Override
    public void setParameter(BeforeEvent event, String reservationCode) {
        removeAll();
        message.removeAll();
        checkOutService.findReservation(reservationCode).ifPresentOrElse(this::show, () -> {
            Paragraph notFound = new Paragraph(ReservationView.NOT_FOUND);
            notFound.setId("reservation-not-found");
            add(new H1("Check Out"), notFound);
        });
    }

    /**
     * Steps 2 to 4, A1 to A3.
     */
    private void show(ChangeableReservation reservation) {
        this.reservation = reservation;
        add(new H1("Check Out " + reservation.reservationCode()), message);

        CheckOutPreview preview;
        try {
            preview = checkOutService.preview(reservation);
        } catch (CheckOutNotPossibleException e) {
            // A1
            showNotCheckedIn(e);
            return;
        }

        add(field("room", "Room", preview.roomNumber()),
                field("arrival-date", "Arrival date", dateFormatter.format(preview.arrivalDate())));
        if (preview.kind() == CheckOutPreview.Kind.ON_TIME) {
            add(field("departure-date", "Departure date", dateFormatter.format(preview.newDepartureDate())),
                    field("nights", "Number of nights", String.valueOf(preview.nights())),
                    field("total-price", "Total price to be paid", chf(preview.newTotalPrice())));
        } else {
            // A2 and A3: the reserved and the new departure date and total price
            showInfo(preview.kind() == CheckOutPreview.Kind.EARLY ? EARLY : OVERDUE);
            add(field("reserved-departure-date", "Reserved departure date",
                            dateFormatter.format(preview.reservedDepartureDate())),
                    field("departure-date", "New departure date", dateFormatter.format(preview.newDepartureDate())),
                    field("nights", "Number of nights", String.valueOf(preview.nights())),
                    field("reserved-total-price", "Reserved total price", chf(preview.reservedTotalPrice())),
                    field("total-price", "Total price to be paid", chf(preview.newTotalPrice())));
        }

        Button confirmButton = new Button("Confirm check-out", _ -> checkOut());
        confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A4: the receptionist does not confirm; the reservation stays unchanged
        Button cancelButton = new Button("Cancel", _ -> showReservation());
        cancelButton.addThemeVariants(ButtonVariant.TERTIARY);
        add(new HorizontalLayout(confirmButton, cancelButton));
    }

    /**
     * Steps 5 to 7, A5.
     */
    private void checkOut() {
        CheckOutPreview result;
        try {
            result = checkOutService.checkOut(reservation.reservationCode());
        } catch (CheckOutNotPossibleException e) {
            // A5: refuse and display the current status
            removeAll();
            message.removeAll();
            add(new H1("Check Out " + reservation.reservationCode()), message);
            showNotCheckedIn(e);
            return;
        }

        // Step 7: the total price of the stay
        removeAll();
        add(new H1("Guest checked out"));
        Paragraph total = new Paragraph("Total price of the stay: " + chf(result.newTotalPrice()));
        total.setId("total-price");
        total.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD);
        // UC-010 BR-004: The system records no payment.
        Paragraph payment = new Paragraph(NO_PAYMENT_RECORDED);
        payment.setId("payment-note");
        add(total, payment, field("room", "Room", result.roomNumber() + " is now dirty and ready for housekeeping"),
                new HorizontalLayout(
                        new Button("Show reservation", _ -> showReservation()),
                        new Button("Back to arrivals and departures",
                                _ -> UI.getCurrent().navigate(ArrivalsAndDeparturesView.class))));
    }

    private void showNotCheckedIn(CheckOutNotPossibleException e) {
        Paragraph error = new Paragraph(NOT_CHECKED_IN.formatted(ReservationView.statusText(e.status())));
        error.setId("error-message");
        error.getElement().setAttribute("role", "alert");
        error.addClassNames(LumoUtility.TextColor.ERROR);
        message.add(error);
        Button backButton = new Button("Back to reservation", _ -> showReservation());
        backButton.addThemeVariants(ButtonVariant.TERTIARY);
        add(backButton);
    }

    private void showInfo(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.setId("check-out-message");
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

    private static String chf(BigDecimal amount) {
        return "CHF " + amount.setScale(2).toPlainString();
    }
}
