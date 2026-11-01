package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import jakarta.annotation.security.RolesAllowed;

import java.math.BigDecimal;

@Route(value = RoomTypeView.ROUTE, layout = StaffLayout.class)
@PageTitle("Room types")
@RolesAllowed("MANAGER")
@AccessDeniedErrorRouter
public class RoomTypeView extends VerticalLayout {

    public static final String ROUTE = "room-types";

    private final RoomService roomService;
    private final Grid<RoomTypeRecord> grid = new Grid<>();
    private final TextField name = new TextField("Name");
    private final IntegerField capacity = new IntegerField("Capacity");
    private final BigDecimalField nightlyRate = new BigDecimalField("Nightly rate");
    private final Binder<RoomTypeRecord> binder = new Binder<>();
    private Long editedId;

    public RoomTypeView(RoomService roomService) {
        this.roomService = roomService;

        grid.setId("room-types");
        grid.addColumn(RoomTypeRecord::getName).setHeader("Name");
        grid.addColumn(RoomTypeRecord::getCapacity).setHeader("Capacity");
        grid.addColumn(t -> formatRate(t.getNightlyRate())).setHeader("Nightly rate");
        grid.setAllRowsVisible(true);
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        name.setMaxLength(50);
        nightlyRate.setPrefixComponent(new Span("CHF"));
        binder.forField(name).asRequired("Name is required")
                .withValidator(v -> !v.isBlank(), "Name is required")
                .bind(RoomTypeRecord::getName, RoomTypeRecord::setName);
        binder.forField(capacity).asRequired("Capacity is required")
                .withValidator(c -> c >= 1, "Capacity must be at least 1")
                .bind(RoomTypeRecord::getCapacity, RoomTypeRecord::setCapacity);
        binder.forField(nightlyRate).asRequired("Nightly rate is required")
                .withValidator(r -> r.signum() > 0, "Nightly rate must be greater than zero")
                .withValidator(r -> r.stripTrailingZeros().scale() <= 2, "Nightly rate has at most two decimals")
                .bind(RoomTypeRecord::getNightlyRate, RoomTypeRecord::setNightlyRate);

        Button save = new Button("Save", _ -> save());
        save.setId("save-room-type");
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button newType = new Button("New room type", _ -> grid.asSingleSelect().clear());
        newType.setId("new-room-type");

        add(new H2("Room types"), grid, new FormLayout(name, capacity, nightlyRate), new HorizontalLayout(save, newType));
        refresh();
    }

    private void edit(RoomTypeRecord roomType) {
        editedId = roomType == null ? null : roomType.getId();
        binder.readBean(roomType);
    }

    private void save() {
        RoomTypeRecord roomType = new RoomTypeRecord();
        if (!binder.writeBeanIfValid(roomType)) {
            return;
        }
        if (editedId != null) {
            roomType.setId(editedId);
        }
        try {
            roomService.saveRoomType(roomType);
            refresh();
        } catch (RoomService.DuplicateException e) {
            Notification.show(e.getMessage()).addThemeVariants(NotificationVariant.ERROR);
        }
    }

    private void refresh() {
        grid.setItems(roomService.findAllRoomTypes());
        edit(null);
    }

    public static String formatRate(BigDecimal rate) {
        return "CHF " + rate.setScale(2);
    }
}
