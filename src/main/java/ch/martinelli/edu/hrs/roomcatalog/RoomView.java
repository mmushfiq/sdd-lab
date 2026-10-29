package ch.martinelli.edu.hrs.roomcatalog;

import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@PageTitle("Rooms")
@Route("rooms")
public class RoomView extends HorizontalLayout {

    private final RoomCatalogService roomCatalogService;

    private final Grid<RoomListItem> grid = new Grid<>();
    private final TextField number = new TextField("Room number");
    private final ComboBox<RoomTypeRecord> roomType = new ComboBox<>("Room type");
    private final Binder<RoomRecord> binder = new Binder<>();
    private final Button delete = new Button("Delete", _ -> confirmDelete());

    private Map<Long, RoomTypeRecord> roomTypesById = Map.of();
    // null while a new room is being entered
    private RoomListItem selected;

    public RoomView(RoomCatalogService roomCatalogService) {
        this.roomCatalogService = roomCatalogService;
        setSizeFull();

        grid.addColumn(RoomListItem::number).setHeader("Number");
        grid.addColumn(RoomListItem::roomTypeName).setHeader("Room type");
        grid.addColumn(RoomListItem::capacity).setHeader("Capacity");
        grid.addColumn(RoomListItem::nightlyRate).setHeader("Nightly rate (CHF)");
        grid.addColumn(r -> r.outOfService() ? "Out of service" : "In service").setHeader("State");
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        roomType.setItemLabelGenerator(RoomTypeRecord::getName);
        var outOfService = new Checkbox("Out of service");
        bindFields(outOfService);

        var save = new Button("Save", _ -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        delete.addThemeVariants(ButtonVariant.ERROR);
        var form = new VerticalLayout(new FormLayout(number, roomType, outOfService),
                new HorizontalLayout(save, new Button("New", _ -> grid.deselectAll()), delete));
        form.setWidth("24rem");

        add(grid, form);
        refresh();
    }

    private void bindFields(Checkbox outOfService) {
        number.setRequiredIndicatorVisible(true);
        binder.forField(number)
                .withConverter(String::trim, v -> v == null ? "" : v)
                .withValidator(v -> !v.isEmpty(), "Room number is required")
                .withValidator(v -> v.length() <= 10, "Room number can have at most 10 characters")
                .bind(RoomRecord::getNumber, RoomRecord::setNumber);
        binder.forField(roomType)
                .asRequired("Room type is required")
                .withConverter(t -> t == null ? null : t.getId(), id -> id == null ? null : roomTypesById.get(id))
                .bind(RoomRecord::getRoomTypeId, RoomRecord::setRoomTypeId);
        binder.forField(outOfService)
                .bind(RoomRecord::getOutOfService, RoomRecord::setOutOfService);
    }

    private void edit(RoomListItem room) {
        selected = room;
        binder.readBean(room == null ? null
                : new RoomRecord(room.id(), room.number(), room.roomTypeId(), room.outOfService()));
        delete.setEnabled(room != null);
    }

    private void save() {
        var room = new RoomRecord();
        if (!binder.writeBeanIfValid(room)) {
            return;
        }
        if (selected != null) {
            room.setId(selected.id());
        }
        try {
            roomCatalogService.saveRoom(room);
            refresh();
        } catch (RoomNumberExistsException e) {
            number.setErrorMessage(e.getMessage());
            number.setInvalid(true);
        }
    }

    private void confirmDelete() {
        var dialog = new ConfirmDialog("Delete room", "Delete room %s?".formatted(selected.number()),
                "Delete", _ -> {
                    roomCatalogService.deleteRoom(selected.id());
                    refresh();
                });
        dialog.setCancelable(true);
        dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    private void refresh() {
        var roomTypes = roomCatalogService.findRoomTypes();
        roomTypesById = roomTypes.stream().collect(Collectors.toMap(RoomTypeRecord::getId, Function.identity()));
        roomType.setItems(roomTypes);
        grid.setItems(roomCatalogService.findRooms());
        edit(null);
    }
}
