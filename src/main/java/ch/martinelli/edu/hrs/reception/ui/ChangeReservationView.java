package ch.martinelli.edu.hrs.reception.ui;

import ch.martinelli.edu.hrs.reservation.ChangePreview;
import ch.martinelli.edu.hrs.reservation.ChangeResult;
import ch.martinelli.edu.hrs.reservation.ChangeableReservation;
import ch.martinelli.edu.hrs.reservation.InvalidChangeException;
import ch.martinelli.edu.hrs.reservation.NightsNotAvailableException;
import ch.martinelli.edu.hrs.reservation.ReservationChange;
import ch.martinelli.edu.hrs.reservation.ReservationChangeService;
import ch.martinelli.edu.hrs.reservation.ReservationNotChangeableException;
import ch.martinelli.edu.hrs.reservation.ReservationStatus;
import ch.martinelli.edu.hrs.reservation.RoomTypeChoice;
import ch.martinelli.edu.hrs.staff.StaffRole;
import ch.martinelli.edu.hrs.staff.ui.StaffLayout;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC-005 Change Reservation: the receptionist changes the dates, room type or number of guests of a reservation
 * opened from the reservation search (UC-007).
 * <p>
 * Nothing is stored before the receptionist confirms, so leaving the view discards the entered values (A6).
 */
@Route(value = "reservation-change", layout = StaffLayout.class)
@PageTitle("Change Reservation")
// NFR-009: Only the receptionist can change reservations.
@RolesAllowed(StaffRole.RECEPTIONIST_ROLE)
public class ChangeReservationView extends VerticalLayout implements HasUrlParameter<String> {

    static final String NOT_CHANGEABLE = "A reservation with status %s cannot be changed.";
    static final String NOT_AVAILABLE = "The room type is not available for these nights: %s.";
    static final String NO_LONGER_AVAILABLE = "The room type is no longer available for these nights: %s.";
    static final String CHANGED = "The reservation has been changed.";
    static final String NOTHING_CHANGED = "Nothing was changed.";

    private final ReservationChangeService changeService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final DatePicker arrivalDate = new DatePicker("Arrival date");
    private final DatePicker departureDate = new DatePicker("Departure date");
    private final Select<RoomTypeChoice> roomType = new Select<>();
    private final IntegerField numberOfGuests = new IntegerField("Number of guests");
    private final Div message = new Div();
    private final VerticalLayout preview = new VerticalLayout();

    private ChangeableReservation reservation;

    public ChangeReservationView(ReservationChangeService changeService) {
        this.changeService = changeService;
        setMaxWidth("60rem");
    }

    @Override
    public void setParameter(BeforeEvent event, String reservationCode) {
        removeAll();
        message.removeAll();
        preview.removeAll();
        changeService.findReservation(reservationCode).ifPresentOrElse(this::show, () -> {
            Paragraph notFound = new Paragraph(ReservationView.NOT_FOUND);
            notFound.setId("reservation-not-found");
            add(new H1("Change Reservation"), notFound);
        });
    }

    /**
     * Step 2, A1, A2 step 1.
     */
    private void show(ChangeableReservation reservation) {
        this.reservation = reservation;
        add(new H1("Change Reservation " + reservation.reservationCode()));
        add(field("status", "Status", ReservationView.statusText(reservation.status())),
                field("arrival-date", "Arrival date", dateFormatter.format(reservation.arrivalDate())),
                field("departure-date", "Departure date", dateFormatter.format(reservation.departureDate())),
                field("room-type", "Room type", reservation.roomTypeName()),
                field("number-of-guests", "Number of guests", String.valueOf(reservation.numberOfGuests())),
                field("total-price", "Total price", chf(reservation.totalPrice())));
        // A2 step 1: the assigned room of a checked-in guest
        if (reservation.roomNumber() != null) {
            add(field("room", "Room", reservation.roomNumber()));
        }

        message.setWidthFull();
        message.getElement().setAttribute("aria-live", "polite");

        Button backButton = new Button("Back to reservation", _ -> showReservation());
        backButton.addThemeVariants(ButtonVariant.TERTIARY);

        if (!ReservationChangeService.isChangeable(reservation.status())) {
            // A1: read-only, and the receptionist is informed
            add(message, backButton);
            showError(NOT_CHANGEABLE.formatted(ReservationView.statusText(reservation.status())));
            return;
        }

        add(new H2("New values"), createForm(), message, preview);
        preview.setPadding(false);
        preview.setVisible(false);

        Button checkButton = new Button("Check change", _ -> check());
        checkButton.addThemeVariants(ButtonVariant.PRIMARY);
        // A6: leaving without confirming discards the entered values
        Button discardButton = new Button("Discard", _ -> showReservation());
        discardButton.addThemeVariants(ButtonVariant.TERTIARY);
        add(new HorizontalLayout(checkButton, discardButton));
    }

    private FormLayout createForm() {
        roomType.setLabel("Room type");
        roomType.setItemLabelGenerator(RoomTypeChoice::name);
        List<RoomTypeChoice> roomTypes = changeService.roomTypesFor(reservation);
        roomType.setItems(roomTypes);
        roomTypes.stream()
                .filter(choice -> choice.roomTypeId().equals(reservation.roomTypeId()))
                .findFirst()
                .ifPresent(roomType::setValue);

        arrivalDate.setValue(reservation.arrivalDate());
        departureDate.setValue(reservation.departureDate());
        numberOfGuests.setValue(reservation.numberOfGuests());
        numberOfGuests.setStepButtonsVisible(true);

        if (reservation.status() == ReservationStatus.CHECKED_IN) {
            // A2 step 1: only the departure date can be changed
            arrivalDate.setReadOnly(true);
            roomType.setReadOnly(true);
            numberOfGuests.setReadOnly(true);
            departureDate.setHelperText("After today; leaving today is a check-out");
        } else {
            arrivalDate.setHelperText("Today or up to 365 days ahead");
            departureDate.setHelperText("After the arrival date");
            numberOfGuests.setHelperText("Between 1 and 10, at most the room type's maximum");
        }

        // A changed value makes the shown prices out of date
        arrivalDate.addValueChangeListener(_ -> valuesChanged());
        departureDate.addValueChangeListener(_ -> valuesChanged());
        roomType.addValueChangeListener(_ -> valuesChanged());
        numberOfGuests.addValueChangeListener(_ -> valuesChanged());

        FormLayout form = new FormLayout(arrivalDate, departureDate, roomType, numberOfGuests);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("40rem", 4));
        form.setWidthFull();
        return form;
    }

    /**
     * Steps 4 and 5, A3, A4.
     */
    private void check() {
        clearMessages();
        try {
            ChangePreview result = changeService.check(reservation.reservationCode(), enteredChange());
            showPreview(result);
        } catch (InvalidChangeException e) {
            // A3: mark the invalid value and explain the allowed values
            markInvalid(e);
        } catch (NightsNotAvailableException e) {
            // A4: which nights are not available; the reservation stays unchanged
            showError(NOT_AVAILABLE.formatted(nights(e.nights())));
        } catch (ReservationNotChangeableException e) {
            showError(NOT_CHANGEABLE.formatted(ReservationView.statusText(e.status())));
        }
    }

    private void showPreview(ChangePreview result) {
        preview.removeAll();
        Paragraph oldTotal = new Paragraph("Old total price: " + chf(result.oldTotalPrice()));
        oldTotal.setId("old-total-price");
        Paragraph newTotal = new Paragraph("New total price: " + chf(result.newTotalPrice()));
        newTotal.setId("new-total-price");
        newTotal.addClassNames(LumoUtility.FontWeight.BOLD);
        Button confirmButton = new Button("Confirm change", _ -> confirm());
        confirmButton.addThemeVariants(ButtonVariant.PRIMARY);
        preview.add(oldTotal, newTotal, confirmButton);
        preview.setVisible(true);
    }

    /**
     * Steps 6 to 9, A5, A7, A8.
     */
    private void confirm() {
        clearMessages();
        try {
            ChangeResult result = changeService.change(reservation.reservationCode(), enteredChange());
            // Step 9 and A5: display the reservation
            Notification.show(result.changed() ? CHANGED : NOTHING_CHANGED);
            showReservation();
        } catch (NightsNotAvailableException e) {
            // A7: continue at step 3; the reservation stays unchanged
            showError(NO_LONGER_AVAILABLE.formatted(nights(e.nights())));
        } catch (InvalidChangeException e) {
            markInvalid(e);
        } catch (ReservationNotChangeableException e) {
            showError(NOT_CHANGEABLE.formatted(ReservationView.statusText(e.status())));
        }
    }

    private ReservationChange enteredChange() {
        RoomTypeChoice selectedRoomType = roomType.getValue();
        Integer guests = numberOfGuests.getValue();
        return new ReservationChange(arrivalDate.getValue(), departureDate.getValue(),
                selectedRoomType == null ? null : selectedRoomType.roomTypeId(), guests == null ? 0 : guests);
    }

    private void markInvalid(InvalidChangeException e) {
        HasValidation field = switch (e.field()) {
            case ARRIVAL_DATE -> arrivalDate;
            case DEPARTURE_DATE -> departureDate;
            case ROOM_TYPE -> roomType;
            case NUMBER_OF_GUESTS -> numberOfGuests;
        };
        field.setErrorMessage(e.getMessage());
        field.setInvalid(true);
    }

    private void valuesChanged() {
        preview.setVisible(false);
        preview.removeAll();
    }

    private void clearMessages() {
        message.removeAll();
        preview.setVisible(false);
        List.<HasValidation>of(arrivalDate, departureDate, roomType, numberOfGuests).forEach(field -> {
            field.setInvalid(false);
            field.setErrorMessage(null);
        });
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

    private String nights(List<LocalDate> nights) {
        return nights.stream().map(dateFormatter::format).collect(Collectors.joining(", "));
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
