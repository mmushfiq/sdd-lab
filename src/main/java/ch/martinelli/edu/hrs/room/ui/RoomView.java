package ch.martinelli.edu.hrs.room.ui;

import ch.martinelli.edu.hrs.core.ui.StaffLayout;
import ch.martinelli.edu.hrs.db.tables.records.RoomRecord;
import ch.martinelli.edu.hrs.db.tables.records.RoomTypeRecord;
import ch.martinelli.edu.hrs.room.RoomService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

@Route(value = RoomView.ROUTE, layout = StaffLayout.class)
@PageTitle("Rooms")
@RolesAllowed("MANAGER")
@AccessDeniedErrorRouter
public class RoomView extends VerticalLayout {

    public static final String ROUTE = "rooms";

    private final RoomService roomService;
    private final Grid<RoomRecord> grid = new Grid<>();
    private final TextField roomNumber = new TextField("Room number");
    private final ComboBox<RoomTypeRecord> roomType = new ComboBox<>("Room type");
    private final Binder<RoomRecord> binder = new Binder<>();
    private final Map<Long, RoomTypeRecord> roomTypes;
    private Long editedId;

    public RoomView(RoomService roomService) {
        this.roomService = roomService;
        List<RoomTypeRecord> types = roomService.findAllRoomTypes();
        roomTypes = types.stream()
                .collect(Collectors.toMap(RoomTypeRecord::getId, Function.identity()));

        grid.setId("rooms");
        grid.addColumn(RoomRecord::getRoomNumber).setHeader("Room number");
        grid.addColumn(r -> type(r).getName()).setHeader("Room type");
        grid.addColumn(r -> type(r).getCapacity()).setHeader("Capacity");
        grid.addColumn(r -> RoomTypeView.formatRate(type(r).getNightlyRate())).setHeader("Base rate");
        grid.setAllRowsVisible(true);
        grid.asSingleSelect().addValueChangeListener(e -> edit(e.getValue()));

        roomNumber.setMaxLength(10);
        roomType.setItems(types);
        roomType.setItemLabelGenerator(RoomTypeRecord::getName);
        binder.forField(roomNumber).asRequired("Room number is required")
                .withValidator(v -> !v.isBlank(), "Room number is required")
                .bind(RoomRecord::getRoomNumber, RoomRecord::setRoomNumber);
        binder.forField(roomType).asRequired("Room type is required")
                .bind(r -> r.getRoomTypeId() == null ? null : roomTypes.get(r.getRoomTypeId()),
                        (r, t) -> r.setRoomTypeId(t.getId()));

        Button save = new Button("Save", _ -> save());
        save.setId("save-room");
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button newRoom = new Button("New room", _ -> grid.asSingleSelect().clear());
        newRoom.setId("new-room");

        add(new H2("Rooms"), grid, new FormLayout(roomNumber, roomType), new HorizontalLayout(save, newRoom));
        refresh();
    }

    private RoomTypeRecord type(RoomRecord room) {
        return roomTypes.get(room.getRoomTypeId());
    }

    private void edit(RoomRecord room) {
        editedId = room == null ? null : room.getId();
        binder.readBean(room);
    }

    private void save() {
        RoomRecord room = new RoomRecord();
        if (!binder.writeBeanIfValid(room)) {
            return;
        }
        if (editedId != null) {
            room.setId(editedId);
        }
        try {
            roomService.saveRoom(room);
            refresh();
        } catch (RoomService.DuplicateException e) {
            Notification.show(e.getMessage()).addThemeVariants(NotificationVariant.ERROR);
        }
    }

    private void refresh() {
        grid.setItems(roomService.findAllRooms());
        edit(null);
    }
}
