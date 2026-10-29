package ch.martinelli.edu.hrs.reservation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;

@PageTitle("Availability")
@Route("availability")
public class AvailabilityView extends VerticalLayout {

    private final ReservationService reservationService;

    private final DatePicker arrival = new DatePicker("Arrival", LocalDate.now());
    private final DatePicker departure = new DatePicker("Departure", LocalDate.now().plusDays(1));
    private final IntegerField guests = new IntegerField("Guests");
    private final Grid<AvailableRoom> grid = new Grid<>();
    private final Span noRooms = new Span("No rooms available for these dates");
    // validation only: values are read from the fields when searching
    private final Binder<Object> binder = new Binder<>();

    // the criteria the shown results belong to; the fields may have been edited since
    private StayPeriod searchedPeriod;
    private int searchedGuests;

    public AvailabilityView(ReservationService reservationService) {
        this.reservationService = reservationService;
        setSizeFull();

        guests.setValue(1);
        bindFields();

        var search = new Button("Search", _ -> search());
        search.addThemeVariants(ButtonVariant.PRIMARY);
        var criteria = new HorizontalLayout(arrival, departure, guests, search);
        criteria.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        grid.addColumn(AvailableRoom::number).setHeader("Room");
        grid.addColumn(AvailableRoom::roomTypeName).setHeader("Room type");
        grid.addColumn(AvailableRoom::capacity).setHeader("Capacity");
        grid.addColumn(AvailableRoom::nightlyRate).setHeader("Nightly rate (CHF)");
        grid.addColumn(AvailableRoom::totalPrice).setHeader("Total (CHF)");
        grid.addComponentColumn(room -> new Button("Book", _ -> book(room))).setHeader("");
        grid.setItems(List.of());

        noRooms.setVisible(false);
        add(criteria, noRooms, grid);
    }

    private void bindFields() {
        binder.forField(arrival)
                .asRequired("Arrival is required")
                .withValidator(d -> !d.isBefore(LocalDate.now()), "Arrival must be today or later")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(departure)
                .asRequired("Departure is required")
                .withValidator(d -> arrival.getValue() == null || d.isAfter(arrival.getValue()),
                        "Departure must be after arrival")
                .bind(_ -> null, (_, _) -> {
                });
        binder.forField(guests)
                .asRequired("Guests is required")
                .withValidator(n -> n >= 1, "At least 1 guest")
                .bind(_ -> null, (_, _) -> {
                });
    }

    private void search() {
        if (!binder.validate().isOk()) {
            grid.setItems(List.of());
            noRooms.setVisible(false);
            return;
        }
        searchedPeriod = new StayPeriod(arrival.getValue(), departure.getValue());
        searchedGuests = guests.getValue();
        var rooms = reservationService.searchAvailability(searchedPeriod, searchedGuests);
        grid.setItems(rooms);
        noRooms.setVisible(rooms.isEmpty());
    }

    private void book(AvailableRoom room) {
        new BookingDialog(reservationService, room, searchedPeriod, searchedGuests, this::search).open();
    }
}
