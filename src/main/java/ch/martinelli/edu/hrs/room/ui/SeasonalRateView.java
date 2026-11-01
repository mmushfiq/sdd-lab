package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.db.tables.records.SeasonalRateRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The manager's seasonal rates: a nightly rate for a room type from a first night up to and including a last night.
 */
@Route(value = SeasonalRateView.ROUTE, layout = StaffLayout.class)
@PageTitle("Seasonal rates")
@RolesAllowed("MANAGER")
@AccessDeniedErrorRouter
public class SeasonalRateView extends VerticalLayout {

    public static final String ROUTE = "seasonal-rates";

    private final RoomService roomService;
    private final Grid<SeasonalRateRecord> grid = new Grid<>();
    private final ComboBox<RoomTypeRecord> roomType = new ComboBox<>("Room type");
    private final TextField name = new TextField("Name");
    private final DatePicker firstNight = new DatePicker("First night");
    private final DatePicker lastNight = new DatePicker("Last night");
    private final BigDecimalField nightlyRate = new BigDecimalField("Nightly rate");
    private final Button delete = new Button("Delete", _ -> delete());
    private final Binder<SeasonalRateRecord> binder = new Binder<>();
    private final Map<Long, RoomTypeRecord> roomTypes;
    private Long editedId;

    public SeasonalRateView(RoomService roomService) {
        this.roomService = roomService;
        List<RoomTypeRecord> types = roomService.findAllRoomTypes();
        roomTypes = types.stream().collect(Collectors.toMap(RoomTypeRecord::getId, Function.identity()));

        grid.setId("seasonal-rates");
        grid.addColumn(r -> roomTypes.get(r.getRoomTypeId()).getName()).setHeader("Room type");
        grid.addColumn(SeasonalRateRecord::getName).setHeader("Name");
        grid.addColumn(SeasonalRateRecord::getFirstNight).setHeader("First night");
        grid.addColumn(SeasonalRateRecord::getLastNight).setHeader("Last night");
        grid.addColumn(r -> RoomTypeView.formatRate(r.getNightlyRate())).setHeader("Nightly rate");
        grid.setAllRowsVisible(true);
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        roomType.setItems(types);
        roomType.setItemLabelGenerator(RoomTypeRecord::getName);
        name.setMaxLength(50);
        nightlyRate.setPrefixComponent(new Span("CHF"));
        binder.forField(roomType).asRequired("Room type is required")
                .bind(r -> r.getRoomTypeId() == null ? null : roomTypes.get(r.getRoomTypeId()),
                        (r, t) -> r.setRoomTypeId(t.getId()));
        binder.forField(name).asRequired("Name is required")
                .withValidator(v -> !v.isBlank(), "Name is required")
                .bind(SeasonalRateRecord::getName, (r, v) -> r.setName(v.strip()));
        binder.forField(firstNight).asRequired("First night is required")
                .bind(SeasonalRateRecord::getFirstNight, SeasonalRateRecord::setFirstNight);
        binder.forField(lastNight).asRequired("Last night is required")
                .withValidator(d -> firstNight.getValue() == null || !d.isBefore(firstNight.getValue()),
                        RoomService.LAST_BEFORE_FIRST)
                .bind(SeasonalRateRecord::getLastNight, SeasonalRateRecord::setLastNight);
        binder.forField(nightlyRate).asRequired("Nightly rate is required")
                .withValidator(r -> r.signum() > 0, "Nightly rate must be greater than zero")
                .withValidator(r -> r.stripTrailingZeros().scale() <= 2, "Nightly rate has at most two decimals")
                .bind(SeasonalRateRecord::getNightlyRate, SeasonalRateRecord::setNightlyRate);

        Button save = new Button("Save", _ -> save());
        save.setId("save-seasonal-rate");
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button newRate = new Button("New seasonal rate", _ -> grid.asSingleSelect().clear());
        newRate.setId("new-seasonal-rate");
        delete.setId("delete-seasonal-rate");
        delete.addThemeVariants(ButtonVariant.ERROR);

        add(new H2("Seasonal rates"), grid, new FormLayout(roomType, name, firstNight, lastNight, nightlyRate),
                new HorizontalLayout(save, newRate, delete));
        refresh();
    }

    private void edit(SeasonalRateRecord rate) {
        editedId = rate == null ? null : rate.getId();
        binder.readBean(rate);
        delete.setEnabled(rate != null);
    }

    private void save() {
        SeasonalRateRecord rate = new SeasonalRateRecord();
        if (!binder.writeBeanIfValid(rate)) {
            return;
        }
        if (editedId != null) {
            rate.setId(editedId);
        }
        try {
            roomService.saveSeasonalRate(rate);
            refresh();
        } catch (RoomService.SeasonalRateException e) {
            Notification.show(e.getMessage()).addThemeVariants(NotificationVariant.ERROR);
        }
    }

    private void delete() {
        if (editedId != null) {
            roomService.deleteSeasonalRate(editedId);
            refresh();
        }
    }

    private void refresh() {
        grid.setItems(roomService.findAllSeasonalRates());
        edit(null);
    }
}
